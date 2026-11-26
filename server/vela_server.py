#!/usr/bin/env python3
"""Vela Local Web Bridge — real yt-dlp + FFmpeg backend for Vela Web UI."""

import json
import mimetypes
import os
import re
import shlex
import subprocess
import tempfile
import threading
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

PORT = int(os.environ.get("VELA_PORT", "8787"))
DOWNLOAD_ROOT = Path(os.environ.get("VELA_DIR", Path.home() / "Downloads" / "Vela"))
DOWNLOAD_ROOT.mkdir(parents=True, exist_ok=True)

JOBS: dict[str, dict] = {}
JOBS_LOCK = threading.Lock()


def run_download_job(job_id: str, payload: dict) -> None:
    url = payload.get("url", "").strip()
    mode = payload.get("mode", "VIDEO")
    quality = payload.get("quality", "1080p")
    container = payload.get("container", "MP4").lower()
    audio_preset = payload.get("audioPreset", "MP3 320k")
    clip_start = payload.get("clipStart", "00:00").strip() or "00:00"
    clip_end = payload.get("clipEnd", "00:30").strip() or "00:30"
    custom_args = payload.get("customArgs", "").strip()

    job_dir = DOWNLOAD_ROOT / job_id[:8]
    job_dir.mkdir(parents=True, exist_ok=True)
    out_tpl = str(job_dir / "%(title).80s.%(ext)s")

    cmd = ["yt-dlp", "--newline", "--no-playlist", "--no-check-certificates", "--geo-bypass", "-o", out_tpl]

    if mode == "AUDIO":
        fmt = "mp3"
        if "M4A" in audio_preset:
            fmt = "m4a"
        elif "FLAC" in audio_preset:
            fmt = "flac"
        cmd += ["-x", "--audio-format", fmt, "--audio-quality", "0", "--embed-thumbnail", "--add-metadata"]
        if "Podcast" in audio_preset:
            cmd += ["--postprocessor-args", "ffmpeg:-ac 1 -b:a 64k"]
        elif "Ringtone" in audio_preset:
            cmd += ["--download-sections", "*00:00-00:30"]
    elif mode == "CLIP":
        cmd += [
            "--download-sections", f"*{clip_start}-{clip_end}",
            "--force-keyframes-at-cuts",
            "-f", "bv*[height<=1080]+ba/b[height<=1080]/b",
            "--merge-output-format", "mp4",
        ]
    elif mode == "COMMAND" and custom_args:
        cmd += shlex.split(custom_args)
    else:
        height_map = {"1080p": "1080", "720p": "720", "480p": "480"}
        h = height_map.get(quality)
        fmt_expr = f"bv*[height<={h}]+ba/b[height<={h}]/b" if h else "bv*+ba/b"
        cmd += ["-f", fmt_expr, "--merge-output-format", container, "--add-metadata"]

    cmd.append(url)

    try:
        proc = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        )
        pct_re = re.compile(r"\[download\]\s+(\d+(?:\.\d+)?)%")
        for line in proc.stdout or []:
            m = pct_re.search(line)
            if m:
                pct = float(m.group(1))
                with JOBS_LOCK:
                    JOBS[job_id]["progress"] = min(99.0, pct)
                    JOBS[job_id]["statusLine"] = line.strip()
        code = proc.wait()
        if code != 0:
            raise RuntimeError(f"yt-dlp exited with code {code}")

        files = sorted(
            [p for p in job_dir.iterdir() if p.is_file()],
            key=lambda p: p.stat().st_mtime,
            reverse=True,
        )
        if not files:
            raise RuntimeError("No output file produced")

        out_file = files[0]
        with JOBS_LOCK:
            JOBS[job_id]["status"] = "COMPLETED"
            JOBS[job_id]["progress"] = 100.0
            JOBS[job_id]["filePath"] = str(out_file)
            JOBS[job_id]["filename"] = out_file.name
            JOBS[job_id]["statusLine"] = f"Saved: {out_file.name}"
    except Exception as exc:
        with JOBS_LOCK:
            JOBS[job_id]["status"] = "FAILED"
            JOBS[job_id]["error"] = str(exc)
            JOBS[job_id]["statusLine"] = f"Error: {exc}"


class VelaHandler(BaseHTTPRequestHandler):
    def _cors(self) -> None:
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")

    def _json(self, code: int, data: dict) -> None:
        body = json.dumps(data).encode("utf-8")
        self.send_response(code)
        self._cors()
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self) -> None:
        self.send_response(204)
        self._cors()
        self.end_headers()

    def do_GET(self) -> None:
        path = urlparse(self.path).path
        if path in ("/", "/index.html"):
            index_file = Path(__file__).resolve().parent.parent / "docs" / "index.html"
            if index_file.exists():
                html = index_file.read_bytes()
                self.send_response(200)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self.send_header("Content-Length", str(len(html)))
                self.end_headers()
                self.wfile.write(html)
                return

        if path == "/health":
            ver = subprocess.check_output(["yt-dlp", "--version"], text=True).strip()
            return self._json(200, {"ok": True, "ytdlp": ver})

        if path.startswith("/api/status/"):
            job_id = path.split("/")[-1]
            with JOBS_LOCK:
                job = JOBS.get(job_id)
            if not job:
                return self._json(404, {"error": "Job not found"})
            return self._json(200, {k: v for k, v in job.items() if k != "filePath"})

        if path.startswith("/api/file/"):
            job_id = path.split("/")[-1]
            with JOBS_LOCK:
                job = JOBS.get(job_id)
            if not job or not job.get("filePath"):
                return self._json(404, {"error": "File not ready"})
            fp = Path(job["filePath"])
            if not fp.exists():
                return self._json(404, {"error": "File missing"})
            mime, _ = mimetypes.guess_type(fp.name)
            self.send_response(200)
            self._cors()
            self.send_header("Content-Type", mime or "application/octet-stream")
            self.send_header("Content-Disposition", f'attachment; filename="{fp.name}"')
            self.send_header("Content-Length", str(fp.stat().st_size))
            self.end_headers()
            with fp.open("rb") as f:
                while chunk := f.read(65536):
                    self.wfile.write(chunk)
            return

        self._json(404, {"error": "Not found"})

    def do_POST(self) -> None:
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length) if length > 0 else b"{}"
        try:
            payload = json.loads(raw.decode("utf-8"))
        except Exception:
            return self._json(400, {"error": "Invalid JSON"})

        path = urlparse(self.path).path
        if path == "/api/info":
            url = payload.get("url", "").strip()
            if not url:
                return self._json(400, {"error": "Missing url"})
            try:
                out = subprocess.check_output(
                    ["yt-dlp", "-J", "--no-playlist", "--socket-timeout", "15", url],
                    text=True,
                    stderr=subprocess.DEVNULL,
                )
                info = json.loads(out)
                return self._json(200, {
                    "title": info.get("title") or "Untitled Media",
                    "uploader": info.get("uploader") or info.get("channel") or "Unknown",
                    "thumbnail": info.get("thumbnail"),
                    "duration": info.get("duration"),
                })
            except Exception as exc:
                return self._json(500, {"error": str(exc)})

        if path == "/api/download":
            url = payload.get("url", "").strip()
            if not url:
                return self._json(400, {"error": "Missing url"})
            job_id = str(uuid.uuid4())
            with JOBS_LOCK:
                JOBS[job_id] = {
                    "id": job_id,
                    "status": "RUNNING",
                    "progress": 0.0,
                    "statusLine": "Starting yt-dlp...",
                    "filename": None,
                    "error": None,
                }
            threading.Thread(target=run_download_job, args=(job_id, payload), daemon=True).start()
            return self._json(202, {"jobId": job_id})

        self._json(404, {"error": "Not found"})


if __name__ == "__main__":
    print(f"Vela Local Bridge listening on http://127.0.0.1:{PORT} (saving to {DOWNLOAD_ROOT})")
    ThreadingHTTPServer(("127.0.0.1", PORT), VelaHandler).serve_forever()

