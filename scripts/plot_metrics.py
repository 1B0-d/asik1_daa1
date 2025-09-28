import re, os, sys
from collections import defaultdict
import matplotlib.pyplot as plt

METRICS = ["compares","copies","merges","pivots","recursions","max_depth","time_ms"]
line_re = re.compile(r"^\s*(?P<label>[^\s,]+)\s*,\s*(?P<pairs>.+)$")
pair_re = re.compile(r"\s*([^:,]+)\s*:\s*([^,]+)\s*")

def parse_file(path):
    rows = []
    with open(path, "r", encoding="utf-8") as f:
        for raw in f:
            s = raw.strip()
            if not s: continue
            m = line_re.match(s)
            if not m: continue
            label = m.group("label")
            pairs = dict(pair_re.findall(m.group("pairs")))
            if label.startswith("ms_"): algo = "mergesort"
            elif label.startswith("qs_"): algo = "quicksort"
            elif label.startswith("select_"): algo = "select"
            elif label.startswith("closest_"): algo = "closest"
            else: algo = "unknown"
            row = {"label": label, "algo": algo}
            n = pairs.get("n") or pairs.get("array size")
            if n is None: continue
            row["n"] = int(str(n).strip())
            for k in METRICS:
                val = None
                if k in pairs: val = pairs[k]
                elif k.replace("_"," ") in pairs: val = pairs[k.replace("_"," ")]
                elif k == "time_ms": val = pairs.get("time in ms") or pairs.get("time ms")
                if val is None: row[k] = None
                else:
                    vs = str(val).strip().replace(",", ".")
                    row[k] = float(vs) if any(c in vs for c in ".eE") else int(vs)
            rows.append(row)
    return rows

def agg(rows, field):
    by = defaultdict(list)
    for r in rows:
        v = r.get(field)
        if v is None: continue
        by[r["algo"]].append((r["n"], v))
    for k in by: by[k].sort()
    return by

def plot_xy(by, ylabel, outpng):
    if not by: return
    plt.figure()
    for algo, pts in sorted(by.items()):
        xs = [x for x,_ in pts]; ys = [y for _,y in pts]
        plt.plot(xs, ys, marker="o", label=algo)
    plt.xlabel("n"); plt.ylabel(ylabel)
    plt.legend(); plt.grid(alpha=0.3); plt.tight_layout()
    plt.savefig(outpng); plt.close()

def main():
    src = sys.argv[1] if len(sys.argv) > 1 else "metrics.csv"
    outdir = "results"; os.makedirs(outdir, exist_ok=True)
    rows = parse_file(src)
    if not rows: print("No data parsed from", src); return
    # clean CSV
    clean = os.path.join(outdir, "metrics_clean.csv")
    with open(clean, "w", encoding="utf-8") as w:
        header = ["label","algo","n"] + METRICS
        w.write(",".join(header) + "\n")
        for r in rows:
            w.write(",".join(str(r.get(h,"")) for h in header) + "\n")
    # plots
    plot_xy(agg(rows, "time_ms"),   "time (ms)", os.path.join(outdir, "time_ms.png"))
    plot_xy(agg(rows, "max_depth"), "max depth", os.path.join(outdir, "depth.png"))
    print("Wrote:", clean, "and PNGs in", outdir)

if __name__ == "__main__":
    main()
