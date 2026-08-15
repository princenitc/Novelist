import sys
from pathlib import Path

# Ensure the repository root (one level above tests/) is on sys.path so
# tests can import the `app` package without setting PYTHONPATH.
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
