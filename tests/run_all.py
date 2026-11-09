"""Run all five local source smoke suites sequentially."""
import subprocess
import sys
from pathlib import Path
from test_support import java_executable


def main():
    root = Path(__file__).resolve().parents[1]
    jar = root / "src/ai-shopping-cart-service/target/ai-shopping-cart-service-1.0.0-SNAPSHOT.jar"
    if not jar.is_file():
        raise SystemExit("Build the application first: bash build-azure.sh")
    java_executable()
    for suite in ("smoke.py", "chat_smoke.py", "retrieval_smoke.py",
                  "nutrition_smoke.py", "grounded_chat_smoke.py"):
        print(f"Running {suite}", flush=True)
        subprocess.run([sys.executable, str(root / "tests" / suite)], cwd=root, check=True)
    print("PASS: all five local source smoke suites.")


if __name__ == "__main__":
    main()
