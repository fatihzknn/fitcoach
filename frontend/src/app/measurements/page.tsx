"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, CheckCircle2, Info } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, ApiError, type BodyMeasurementDto, type FitnessProfileDto } from "@/lib/api";
import { cn } from "@/lib/utils";

// ─── BF% category ────────────────────────────────────────────────────────────

function bfCategory(bf: number, sex: string): { label: string; color: string } {
  if (sex === "FEMALE") {
    if (bf < 14) return { label: "Essential fat", color: "text-amber-400" };
    if (bf < 21) return { label: "Athlete",       color: "text-primary" };
    if (bf < 25) return { label: "Fitness",       color: "text-green-400" };
    if (bf < 32) return { label: "Average",       color: "text-yellow-400" };
    return              { label: "Above average", color: "text-red-400" };
  }
  // MALE / OTHER
  if (bf < 6)  return { label: "Essential fat", color: "text-amber-400" };
  if (bf < 14) return { label: "Athlete",       color: "text-primary" };
  if (bf < 18) return { label: "Fitness",       color: "text-green-400" };
  if (bf < 25) return { label: "Average",       color: "text-yellow-400" };
  return              { label: "Above average", color: "text-red-400" };
}

const METHOD_LABEL: Record<string, string> = {
  NAVY: "US Navy method",
  BAI:  "Body Adiposity Index",
};

// ─── Reusable measurement input ───────────────────────────────────────────────

function MeasurementField({
  label, description, id, value, onChange, required, unit = "cm",
}: {
  label: string; description?: string; id: string; value: string;
  onChange: (v: string) => void; required?: boolean; unit?: string;
}) {
  return (
    <div className="space-y-1.5">
      <Label htmlFor={id} className="flex items-center gap-1.5">
        {label}
        {required && <span className="text-primary text-xs">*</span>}
      </Label>
      {description && <p className="text-xs text-muted-foreground">{description}</p>}
      <div className="relative">
        <Input
          id={id} inputMode="decimal" placeholder="–"
          value={value} onChange={(e) => onChange(e.target.value)}
          className="pr-10"
        />
        <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-muted-foreground pointer-events-none">
          {unit}
        </span>
      </div>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function MeasurementsPage() {
  const router = useRouter();

  const [profile, setProfile] = React.useState<FitnessProfileDto | null>(null);
  const [saved,   setSaved]   = React.useState<BodyMeasurementDto | null>(null);
  const [error,   setError]   = React.useState<string | null>(null);
  const [submitting, setSubmitting] = React.useState(false);

  // Measurement fields
  const [weight, setWeight] = React.useState("");
  const [neck,   setNeck]   = React.useState("");
  const [waist,  setWaist]  = React.useState("");
  const [hip,    setHip]    = React.useState("");
  const [chest,  setChest]  = React.useState("");
  const [bicep,  setBicep]  = React.useState("");
  const [thigh,  setThigh]  = React.useState("");
  const [calf,   setCalf]   = React.useState("");

  React.useEffect(() => {
    api.profile().then(setProfile).catch(() => {});
  }, []);

  const isFemale = profile?.sex === "FEMALE";

  // Women: hip alone is enough for BAI. Men: need neck + waist for Navy.
  const canComputeBf = isFemale ? !!hip : !!(neck && waist);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const result = await api.saveMeasurement({
        measuredAt: null,
        weightKg:  weight ? parseFloat(weight) : null,
        neckCm:    neck   ? parseFloat(neck)   : null,
        waistCm:   waist  ? parseFloat(waist)  : null,
        hipCm:     hip    ? parseFloat(hip)    : null,
        chestCm:   chest  ? parseFloat(chest)  : null,
        bicepCm:   bicep  ? parseFloat(bicep)  : null,
        thighCm:   thigh  ? parseFloat(thigh)  : null,
        calfCm:    calf   ? parseFloat(calf)   : null,
        notes:     null,
      });
      setSaved(result);
    } catch (err: unknown) {
      setError(err instanceof ApiError ? err.message : "Could not save measurement.");
    } finally {
      setSubmitting(false);
    }
  }

  function resetForm() {
    setSaved(null);
    setWeight(""); setNeck(""); setWaist(""); setHip("");
    setChest(""); setBicep(""); setThigh(""); setCalf("");
  }

  // ── Success state ──────────────────────────────────────────────────────────
  if (saved) {
    const bf  = saved.bodyFatPercentage != null ? parseFloat(saved.bodyFatPercentage.toString()) : null;
    const cat = bf != null ? bfCategory(bf, profile?.sex ?? "OTHER") : null;
    const methodLabel = saved.bodyFatMethod ? (METHOD_LABEL[saved.bodyFatMethod] ?? saved.bodyFatMethod) : null;

    return (
      <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col items-center justify-center px-4 py-10 space-y-6">
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/15">
          <CheckCircle2 className="h-8 w-8 text-primary" />
        </div>

        <div className="text-center space-y-1">
          <h1 className="font-display text-2xl font-bold">Measurements saved</h1>
          <p className="text-sm text-muted-foreground">{saved.measuredAt}</p>
        </div>

        {/* BF% result */}
        {bf != null && cat ? (
          <div className="w-full rounded-2xl border border-primary/30 bg-primary/5 p-6 text-center space-y-2">
            <p className="text-sm text-muted-foreground">Estimated body fat</p>
            <p className="font-display text-5xl font-extrabold tracking-tight">
              {bf.toFixed(1)}<span className="text-2xl font-bold">%</span>
            </p>
            <span className={cn("inline-block rounded-full px-3 py-1 text-sm font-semibold bg-card border border-border", cat.color)}>
              {cat.label}
            </span>
            {methodLabel && (
              <p className="text-xs text-muted-foreground pt-1">{methodLabel}</p>
            )}
          </div>
        ) : (
          <div className="w-full rounded-2xl border border-border bg-card p-4 text-center">
            <p className="text-sm text-muted-foreground">
              {isFemale
                ? "Measure your hip circumference to estimate body fat (BAI method)."
                : "Measure neck and waist to estimate body fat (US Navy method)."}
            </p>
          </div>
        )}

        {/* Site measurements summary */}
        <div className="w-full grid grid-cols-2 gap-2">
          {[
            { label: "Weight", val: saved.weightKg,   unit: "kg" },
            { label: "Waist",  val: saved.waistCm,    unit: "cm" },
            { label: "Hip",    val: saved.hipCm,      unit: "cm" },
            { label: "Chest",  val: saved.chestCm,    unit: "cm" },
            { label: "Bicep",  val: saved.bicepCm,    unit: "cm" },
            { label: "Thigh",  val: saved.thighCm,    unit: "cm" },
            { label: "Calf",   val: saved.calfCm,     unit: "cm" },
          ].filter((s) => s.val != null).map((s) => (
            <div key={s.label} className="rounded-lg bg-elevated border border-border px-3 py-2 flex justify-between items-center">
              <span className="text-xs text-muted-foreground">{s.label}</span>
              <span className="text-sm font-semibold">{s.val} {s.unit}</span>
            </div>
          ))}
        </div>

        <div className="flex flex-col gap-2 w-full">
          <Button className="w-full" onClick={() => router.push("/progress")}>View progress</Button>
          <Button variant="secondary" className="w-full" onClick={resetForm}>Log another</Button>
        </div>
      </main>
    );
  }

  // ── Form ───────────────────────────────────────────────────────────────────
  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col px-4 py-6">
      <div className="flex items-center gap-3 mb-6">
        <button
          onClick={() => router.back()}
          className="flex h-10 w-10 items-center justify-center rounded-md text-muted-foreground hover:bg-secondary"
          aria-label="Back"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <h1 className="font-display text-2xl font-bold">Body measurements</h1>
          <p className="text-xs text-muted-foreground">Today&apos;s reading</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6 flex-1">

        {/* Method info */}
        <div className="flex gap-2.5 rounded-xl bg-elevated border border-border px-4 py-3">
          <Info className="h-4 w-4 text-muted-foreground flex-shrink-0 mt-0.5" />
          <div className="text-xs text-muted-foreground leading-relaxed space-y-0.5">
            {isFemale ? (
              <>
                <p><strong className="text-foreground">Women</strong> — Body Adiposity Index (BAI): hip + height only. More accurate for women than US Navy (r=0.85 vs DEXA).</p>
                <p>Height from your profile: {profile?.heightCm} cm.</p>
              </>
            ) : (
              <>
                <p><strong className="text-foreground">Men</strong> — US Navy circumference method: neck + waist + height.</p>
                <p>Height from your profile: {profile?.heightCm} cm.</p>
              </>
            )}
          </div>
        </div>

        {/* Weight */}
        <section className="space-y-3">
          <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Weight</h2>
          <MeasurementField
            id="weight" label="Body weight" unit="kg"
            value={weight} onChange={setWeight}
          />
        </section>

        {/* BF% section — different for men vs women */}
        <section className="space-y-3">
          <div className="flex items-center gap-2">
            <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
              Body fat estimate
            </h2>
            <span className="text-xs text-muted-foreground/60">
              {isFemale ? "BAI" : "US Navy"}
            </span>
          </div>

          {isFemale ? (
            // Women: hip alone → BAI
            <MeasurementField
              id="hip" label="Hip circumference" required
              description="Widest point of the hips and buttocks."
              value={hip} onChange={setHip}
            />
          ) : (
            // Men: neck + waist → Navy
            <>
              <MeasurementField
                id="neck" label="Neck" required
                description="Below the larynx (Adam's apple), slightly angled downward."
                value={neck} onChange={setNeck}
              />
              <MeasurementField
                id="waist" label="Waist / abdomen" required
                description="At the navel, relaxed (not sucked in)."
                value={waist} onChange={setWaist}
              />
            </>
          )}

          {!canComputeBf && (isFemale ? hip === "" : neck === "" || waist === "") && (
            <p className="text-xs text-amber-400">
              {isFemale ? "Add hip measurement to estimate body fat." : "Add neck and waist to estimate body fat."}
            </p>
          )}
        </section>

        {/* Optional measurements */}
        <section className="space-y-3">
          <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
            Optional measurements
          </h2>
          <div className="grid grid-cols-2 gap-3">
            {isFemale ? (
              // For women neck + waist are optional (not needed for BAI)
              <>
                <MeasurementField id="waist"  label="Waist"  value={waist}  onChange={setWaist}  />
                <MeasurementField id="neck"   label="Neck"   value={neck}   onChange={setNeck}   />
                <MeasurementField id="chest"  label="Chest"  value={chest}  onChange={setChest}  />
                <MeasurementField id="bicep"  label="Bicep"  value={bicep}  onChange={setBicep}  />
                <MeasurementField id="thigh"  label="Thigh"  value={thigh}  onChange={setThigh}  />
                <MeasurementField id="calf"   label="Calf"   value={calf}   onChange={setCalf}   />
              </>
            ) : (
              // For men hip, chest, bicep, thigh, calf are optional
              <>
                <MeasurementField id="chest"  label="Chest"  value={chest}  onChange={setChest}  />
                <MeasurementField id="bicep"  label="Bicep"  value={bicep}  onChange={setBicep}  />
                <MeasurementField id="thigh"  label="Thigh"  value={thigh}  onChange={setThigh}  />
                <MeasurementField id="calf"   label="Calf"   value={calf}   onChange={setCalf}   />
              </>
            )}
          </div>
        </section>

        {error && (
          <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">
            {error}
          </p>
        )}

        <div className="pb-6">
          <Button type="submit" className="w-full" size="lg" disabled={submitting}>
            {submitting ? "Saving…" : "Save measurements"}
          </Button>
        </div>
      </form>
    </main>
  );
}
