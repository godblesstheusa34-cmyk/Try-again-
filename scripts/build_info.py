"""Write an exact source revision and APK checksum for the GitHub artifact."""
import hashlib, os, subprocess
from pathlib import Path
root = Path(__file__).resolve().parents[1]
apk = root / 'app/build/outputs/apk/debug/app-debug.apk'
sha = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()
info = (
    'Fluid Home debug build\n'
    f'Source commit: {sha}\n'
    f'Workflow run: {os.environ.get("GITHUB_RUN_NUMBER", "local")}\n'
    f'GitHub ref: {os.environ.get("GITHUB_REF", "local")}\n'
    f'APK SHA-256: {hashlib.sha256(apk.read_bytes()).hexdigest()}\n'
    'Toolchain: Java 17 / Gradle 8.11.1 / AGP 8.9.2 / Android API 35\n'
)
apk.with_name('build-info.txt').write_text(info)
print(info)
