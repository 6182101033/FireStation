import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// Eksperimen Tahap 1: 16 model (2 representasi x 2 selection x 2 crossover x 2 mutasi)
// x 12 peta (tiny/small/normal, masing-masing 2 acak + 1 klaster + 1 labirin) x 30 seed = 5760 run.
//
// Cara run (dari folder yang berisi TestCases/):
//   java -cp Kode/MyGA EksperimenTahap1                          -> semua 5760 run ke Hasil_Tahap1_16model.csv
//   java -cp Kode/MyGA EksperimenTahap1 model=1-8 out=a.csv      -> hanya model 1..8 (untuk dibagi ke beberapa komputer)
//   java -cp Kode/MyGA EksperimenTahap1 threads=4 peta=n20 iter=3 -> uji cepat
// Jika file output sudah ada, run yang sudah tercatat dilewati (bisa dilanjutkan setelah terhenti).
public class EksperimenTahap1 {
    // ---- Pengaturan tetap Tahap 1 ----
    static final int NPM_KETUA = 21033;      // NPM ketua: sumber seed (sama untuk semua model)
    static final int POP = 100;              // jumlah populasi
    static final double PC = 0.8;            // peluang crossover
    static final double PM = 0.05;           // peluang mutasi
    static final double ELITISM = 0.20;      // persentase elitism

    // 12 peta Tahap 1: n20 = tiny, n40 = small, n60 = normal
    static final String[] DAFTAR_PETA = {
        // "TestCases/n20_acak_01.txt", "TestCases/n20_acak_02.txt",
        // "TestCases/n20_klaster_01.txt", "TestCases/n20_labirin_01.txt",
        // "TestCases/n40_acak_01.txt", "TestCases/n40_acak_02.txt",
        // "TestCases/n40_klaster_01.txt", "TestCases/n40_labirin_01.txt",
        //"TestCases/n60_acak_01.txt", "TestCases/n60_acak_02.txt",
        "TestCases/n60_klaster_01.txt", "TestCases/n60_labirin_01.txt"
    };

    // Satu pekerjaan = satu run (model, peta, seed ke-iterasi)
    static class Tugas {
        int model;      // nomor model 1..16
        String peta;    // path peta
        int iterasi;    // 1..30
    }

    public static void main(String[] args) throws Exception {
        // ---- 1. Baca argumen key=value ----
        Map<String, String> opsi = new HashMap<>();                                  // wadah argumen
        for (String a : args) {
            int s = a.indexOf('=');                                                  // pisah key dan value
            if (s > 0) opsi.put(a.substring(0, s), a.substring(s + 1));
        }
        int modelMulai = 1, modelAkhir = 16;                                         // default: semua model
        if (opsi.containsKey("model")) {                                             // format "a-b" atau "a"
            String[] r = opsi.get("model").split("-");
            modelMulai = Integer.parseInt(r[0]);
            modelAkhir = (r.length > 1) ? Integer.parseInt(r[1]) : modelMulai;
        }
        String fileOut = opsi.getOrDefault("out", "Hasil_Tahap1_16model.csv");       // file CSV keluaran
        int jumlahThread = Integer.parseInt(opsi.getOrDefault("threads",
                String.valueOf(Math.max(1, Runtime.getRuntime().availableProcessors() - 1)))); // default: semua core - 1
        String filterPeta = opsi.getOrDefault("peta", "");                           // filter nama peta (untuk uji)
        int jumlahIterasi = Integer.parseInt(opsi.getOrDefault("iter", "30"));       // default 30 seed

        // ---- 2. Seed (sama untuk semua model, sesuai soal) ----
        Random rand = new Random(NPM_KETUA);                                         // RNG pembangkit seed
        int[] seed = new int[30];                                                    // 30 seed
        int batas = 1 << 16;                                                         // batas atas 2^16
        for (int i = 0; i < seed.length; i++) seed[i] = rand.nextInt(batas + 1);

        // ---- 3. Muat tiap peta sekali saja (read-only, aman dipakai banyak thread) ----
        Map<String, EvaluatorPeta> evaluator = new HashMap<>();                      // peta -> evaluator
        List<String> petaDipakai = new ArrayList<>();                                // daftar peta yang dijalankan
        for (String peta : DAFTAR_PETA) {
            if (!peta.contains(filterPeta)) continue;                                // lewati jika tidak cocok filter
            EvaluatorPeta ev = new EvaluatorPeta();
            ev.muatPeta(peta);                                                       // baca peta
            ev.hitungMatriksJarak();                                                 // BFS sekali di awal
            evaluator.put(peta, ev);
            petaDipakai.add(peta);
        }

        // ---- 4. Siapkan CSV; jika sudah ada, catat run yang sudah selesai ----
        File f = new File(fileOut);
        Set<String> sudahSelesai = new HashSet<>();                                  // kunci "model|peta|iterasi"
        boolean fileBaru = !f.exists();
        if (!fileBaru) {
            for (String baris : Files.readAllLines(f.toPath())) {
                String[] k = baris.split(",");
                if (k.length > 8 && !k[0].equals("Model_ID")) sudahSelesai.add(k[0] + "|" + k[7] + "|" + k[8]);
            }
        }
        PrintWriter csv = new PrintWriter(new FileWriter(f, true));                  // mode append
        if (fileBaru) {
            csv.println("Model_ID,Representasi,Selection,Crossover,Mutasi,Tipe_Kasus,Jenis_Peta,Peta,Iterasi,Seed,"
                    + "Populasi,Pc,Pm,Elitism,Evaluasi_Budget,Generasi,Cost_Terbaik,Waktu_ms");
            csv.flush();
        }

        // ---- 5. Susun daftar pekerjaan ----
        List<Tugas> daftar = new ArrayList<>();
        for (int model = modelMulai; model <= modelAkhir; model++) {
            for (String peta : petaDipakai) {
                for (int it = 1; it <= jumlahIterasi; it++) {
                    if (sudahSelesai.contains(model + "|" + peta + "|" + it)) continue; // sudah ada di CSV
                    Tugas t = new Tugas();
                    t.model = model; t.peta = peta; t.iterasi = it;
                    daftar.add(t);
                }
            }
        }
        System.out.println("Run yang akan dikerjakan: " + daftar.size() + " | thread: " + jumlahThread + " | output: " + fileOut);

        // ---- 6. Jalankan paralel; tiap run punya RNG, GA, dan populasi sendiri ----
        ExecutorService pool = Executors.newFixedThreadPool(jumlahThread);
        AtomicInteger selesai = new AtomicInteger(0);                                // penghitung progres
        final int total = daftar.size();
        for (Tugas t : daftar) {
            pool.submit(() -> {
                try {
                    String baris = jalankan(t, evaluator.get(t.peta), seed[t.iterasi - 1]);
                    synchronized (csv) {                                             // tulis satu baris, aman antar thread
                        csv.println(baris);
                        csv.flush();
                    }
                } catch (Throwable e) {
                    System.err.println("GAGAL model " + t.model + " " + t.peta + " iter " + t.iterasi + ": " + e);
                }
                int n = selesai.incrementAndGet();
                if (n % 50 == 0 || n == total) System.out.println("Progres: " + n + "/" + total);
            });
        }
        pool.shutdown();
        pool.awaitTermination(7, TimeUnit.DAYS);
        csv.close();
        System.out.println("Selesai. Hasil: " + fileOut);
    }

    // Menjalankan satu GA lengkap dan mengembalikan satu baris CSV
    static String jalankan(Tugas t, EvaluatorPeta ev, int seedRun) {
        // Decode nomor model (1..16) menjadi 4 pilihan operator: bit tertinggi = representasi, terendah = mutasi
        int idx = t.model - 1;
        int repr = (idx >> 3) & 1;       // 0 = Bit, 1 = FSOnly
        int sel = (idx >> 2) & 1;        // 0 = Tournament, 1 = Roulette
        int cx = (idx >> 1) & 1;         // 0 = SinglePoint, 1 = Uniform
        int mut = idx & 1;               // 0 = mutasi A, 1 = mutasi B

        // Budget evaluasi sesuai ukuran peta
        int budget = 10000;                                  // tiny (n20)
        String tipe = "tiny";
        if (t.peta.contains("n40")) { budget = 50000; tipe = "small"; }
        if (t.peta.contains("n60")) { budget = 200000; tipe = "normal"; }
        int maxGen = budget / POP;                           // generasi = budget / populasi
        String jenis = t.peta.contains("acak") ? "acak" : (t.peta.contains("klaster") ? "klaster" : "labirin");

        long mulai = System.currentTimeMillis();             // catat waktu mulai
        Random gen = new Random(seedRun);                    // RNG khusus run ini
        int p = ev.p;                                        // jumlah FS
        int panjang = ev.daftarKosong.size();                // jumlah sel kosong
        FireStationGA ga = new FireStationGA(POP, maxGen, PC, PM, ELITISM, repr, sel, cx, mut);
        Population pop = new Population(POP, panjang, p, ELITISM, ev, gen, repr);   // populasi awal
        for (int g = 1; g <= maxGen; g++) {                  // evolusi sampai budget habis
            pop = ga.evolve(pop, panjang, p, ev, gen);
        }
        double cost = pop.getBestIndividual().getCost();     // cost terbaik akhir
        long waktu = System.currentTimeMillis() - mulai;     // lama run (ms)

        return t.model + "," + FireStationGA.NAMA_REPR[repr] + "," + FireStationGA.NAMA_SEL[sel] + ","
                + FireStationGA.NAMA_CX[cx] + "," + FireStationGA.namaMutasi(repr, mut) + ","
                + tipe + "," + jenis + "," + t.peta + "," + t.iterasi + "," + seedRun + ","
                + POP + "," + PC + "," + PM + "," + ELITISM + "," + budget + "," + maxGen + ","
                + String.format(Locale.US, "%.5f", cost) + "," + waktu;
    }
}
