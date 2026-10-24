#!/usr/bin/env python3
"""Preseed duplicate icon objects to avoid ForgeGradle 3's concurrent writes."""
import hashlib
import os
from pathlib import Path
import subprocess
import tempfile

cache = Path(os.environ.get("GRADLE_USER_HOME", "/workspace/.gradle")) / "caches/forge_gradle/assets/objects"
# Immutable object hashes from Mojang's Minecraft 1.12 asset index.
for digest in (
    "5ff04807c356f1beed0b86ccf659b44b9983e3fa",
    "af96f55a90eaf11b327f1b5f8834a051027dc506",
    "f00657542252858a721e715a2e888a9226404e35",
):
    target = cache / digest[:2] / digest
    if target.exists() and hashlib.sha1(target.read_bytes()).hexdigest() == digest:
        continue
    target.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=target.parent, delete=False) as temp:
        temporary = Path(temp.name)
    try:
        subprocess.run([
            "curl", "--fail", "--location", "--retry", "2", "--max-time", "60",
            "https://resources.download.minecraft.net/" + digest[:2] + "/" + digest,
            "--output", str(temporary),
        ], check=True)
        if hashlib.sha1(temporary.read_bytes()).hexdigest() != digest:
            raise RuntimeError("Minecraft icon object failed its published SHA-1 check")
        temporary.replace(target)
    finally:
        if temporary.exists():
            temporary.unlink()
print("Three shared Minecraft icon objects verified")
