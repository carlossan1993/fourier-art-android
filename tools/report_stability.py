#!/usr/bin/env python3
"""Print reproducible CI observations; timing/memory are not device-wide guarantees."""
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

root = Path(sys.argv[1])
report = {"devices": {}, "unitTests": {}}
for api, folder in [(29, root), (35, root / "api35"), (24, root / "api24")]:
    output = (folder / "instrumentation.txt").read_text()
    passed = re.search(r"OK \((\d+) tests?\)", output)
    assert passed, f"API {api}: instrumentation failed or timed out"
    observations = {}
    for name in ("input", "navigation", "performance", "cancellation"):
        path = folder / "stability" / (name + ".txt")
        assert path.is_file(), f"Missing API {api} evidence: {path}"
        observations[name] = path.read_text()
    report["devices"][str(api)] = {"passed": int(passed.group(1)), "observations": observations}
suites = [ET.parse(path).getroot() for path in Path("app/build/test-results/testDebugUnitTest").glob("TEST-*.xml")]
assert suites, "No unit test reports"
for key in ("tests", "failures", "errors", "skipped"):
    report["unitTests"][key] = sum(int(s.get(key, "0")) for s in suites)
assert report["unitTests"]["failures"] == report["unitTests"]["errors"] == 0
for api in (29, 35):
    data = json.loads((root / f"export-validation-api{api}.json").read_text())
    assert len(data) == 6
report["externalExportFilesVerified"] = 12
report["limits"] = "Debug emulators; software render cost and main-thread heartbeat are not screen FPS. Physical device matrix and recognizability bank remain pending."
rendered = json.dumps(report, indent=2)
(root / "stage8-summary.json").write_text(rendered)
print(rendered)
