"""
Verify Python environment for ActionGenerator integration tests
"""
import sys
import os

# Add tau_bench to path
tau_bench_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "amazon-tau-bench-tasks-main"))
if os.path.exists(tau_bench_path):
    sys.path.insert(0, tau_bench_path)
    print(f"Added to Python path: {tau_bench_path}")
else:
    print(f"Warning: tau_bench path not found: {tau_bench_path}")

def check_python_version():
    print(f"Python version: {sys.version}")
    major, minor = sys.version_info[:2]
    if major >= 3 and minor >= 7:
        print("✓ Python version OK (>= 3.7)")
        return True
    else:
        print("✗ Python version too old. Need Python 3.7+")
        return False

def check_tau_bench():
    try:
        import tau_bench
        print(f"✓ tau_bench module found")
        print(f"  Location: {tau_bench.__file__}")
        return True
    except ImportError as e:
        print(f"✗ tau_bench module not found: {e}")
        print("  Install with: pip install tau-bench")
        return False

def check_tool_base():
    try:
        from tau_bench.envs.tool import Tool
        print("✓ tau_bench.envs.tool.Tool import successful")
        return True
    except ImportError as e:
        print(f"✗ Cannot import Tool base class: {e}")
        return False

def main():
    print("=" * 60)
    print("ActionGenerator Integration Test Environment Check")
    print("=" * 60)
    
    checks = [
        ("Python Version", check_python_version),
        ("tau_bench Package", check_tau_bench),
        ("Tool Base Class", check_tool_base)
    ]
    
    results = []
    for name, check_func in checks:
        print(f"\n{name}:")
        result = check_func()
        results.append(result)
    
    print("\n" + "=" * 60)
    if all(results):
        print("✓ All checks passed! Integration tests can run.")
        print("\nNext steps:")
        print("1. Set environment variable: $env:ENABLE_INTEGRATION_TESTS='true'")
        print("2. Run tests: mvn test -Dtest=ActionGeneratorIntegrationTest")
        return 0
    else:
        print("✗ Some checks failed. Fix the issues above.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
