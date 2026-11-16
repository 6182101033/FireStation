import java.util.Random;

public class Main {
    public static void main(String[] args) {
        // --- 1. SETTING HYPERPARAMETER AWAL (Dari instruksi Tahap 1) ---
        int maxPopulationSize = 100;
        int totalGeneration = 100; // Contoh: budget 10.000 / populasi 100
        double crossoverRate = 0.8;
        double mutationRate = 0.05;
        double elitismPct = 0.20;

        // Mempersiapkan seed random seperti instruksi dosen
        int npm_ketua = 21033; // NPM ketua (sama dengan EksperimenTahap1)
        Random randInit = new Random(npm_ketua);
        int[] seeds = new int[30];
        int batas = 1 << 16;
        for (int i = 0; i < seeds.length; i++) {
            seeds[i] = randInit.nextInt(batas + 1);
        }

        // --- 2. MEMUAT PETA & PRE-KOMPUTASI JARAK ---
        String filePeta = "TestCases/n20_labirin_01.txt";
        EvaluatorPeta evaluator = new EvaluatorPeta();
        evaluator.muatPeta(filePeta);
        evaluator.hitungMatriksJarak();

        int p = evaluator.p;
        int panjangKromosom = evaluator.daftarKosong.size();

        System.out.println("\n--- MEMULAI EKSPERIMEN GA FIRE STATION ---");
        System.out.println("Peta: " + filePeta);

        double totalCostSemuaPercobaan = 0;

        // --- 3. LOOPING EKSPERIMEN 30 KALI ---
        // Jumlah loop bisa diubah menjadi 1 dulu untuk tes cepat
        int jumlahPercobaan = 1;

        for (int ct = 0; ct < jumlahPercobaan; ct++) {
            int seedSaatIni = seeds[ct];
            Random gen = new Random(seedSaatIni);

            /*
             * --- BAGIAN INI MASIH DI-COMMENT SAMPAI FIRESTATIONGA SELESAI DIBUAT ---
             * --------------------------------------------------------------------------
             */
            FireStationGA ga = new FireStationGA(maxPopulationSize, totalGeneration, crossoverRate, mutationRate,
                    elitismPct);

            // Buat populasi awal
            Population pop = new Population(maxPopulationSize, panjangKromosom, p, elitismPct, evaluator, gen);

            // Proses Evolusi
            // for (int genIdx = 1; genIdx <= totalGeneration; genIdx++) {
            // pop = ga.evolve(pop, panjangKromosom, p, evaluator, gen);
            // }

            // sebelum loop: catat generasi 0 (populasi awal)
            System.out.printf("Gen   0 | best cost %.5f%n", pop.listOfIndividual.get(0).getCost());

            for (int genIdx = 1; genIdx <= totalGeneration; genIdx++) {
                pop = ga.evolve(pop, panjangKromosom, p, evaluator, gen);

                double best = pop.listOfIndividual.get(0).getCost(); // sudah terurut, indeks 0 = terbaik
                if (genIdx % 10 == 0) // cetak tiap 10 generasi saja
                    System.out.printf("Gen %3d | best cost %.5f%n", genIdx, best);
            }

            // Ambil individu terbaik dari percobaan ini
            Individual res = pop.getBestIndividual();
            double costTerbaik = res.getCost();
            totalCostSemuaPercobaan += costTerbaik;

            System.out.println(p + " " + String.format(java.util.Locale.US, "%.5f", costTerbaik)); // baris 1: p dan cost
            for (int id : res.getFireStationIndices()) {                   // tiap FS terpilih
                EvaluatorPeta.Titik t = evaluator.daftarKosong.get(id);    // id → posisi di grid
                System.out.println((t.c + 1) + " " + (t.r + 1));           // "x y" = (kolom+1) (baris+1)
            }

            System.out.printf("Run %2d | Seed: %5d | Cost Terbaik: %.5f\n", (ct + 1), seedSaatIni, costTerbaik);
        }

        // System.out.printf("\nRata-rata Cost dari %d percobaan: %.5f\n",
        // jumlahPercobaan, (totalCostSemuaPercobaan/jumlahPercobaan));
        System.out.println("Pre-komputasi selesai. Menunggu implementasi FireStationGA.");
    }
}
// kode program tidak terpakai, kayaknya buat coba2

// Individual idv = new Individual(gen,1282);
// idv.setFitness(listOfItems,maxCapacity);
// System.out.println("-----------");
// System.out.println(idv);
// System.out.println("-----------");

// System.out.println(res.generation);
// System.out.println(res.bestindividual);
/*
 * int value=0;
 * int weight=0;
 * int sol = 1283;
 * for (int i=0;i<Integer.SIZE;i++) {
 * int bit = sol&(1<<i);
 * System.out.println(i+" :"+((bit>0)?"1":"0"));
 * }
 * for (int i=0;i<Integer.SIZE;i++) {
 * int bit = sol&(1<<i);
 * System.out.println(sol);
 * System.out.println(bit);
 * if (bit!=0) {
 * value = value + listOfItems.get(i).value;
 * weight = weight + listOfItems.get(i).weight;
 * System.out.println(
 * listOfItems.get(i).name+" "+listOfItems.get(i).weight+" "+listOfItems.get(i).
 * value);
 * }
 * }
 * System.out.println(weight+" : "+value);
 */

/*
 * int b = 1283;
 * int c = 832748375;
 * //System.out.println(Integer.SIZE);
 * for (int i=0;i<Integer.SIZE;i++) {
 * int bit = b&(1<<i);
 * System.out.println(i+" :"+bit);
 * }
 * System.out.println("========================");
 * for (int i=0;i<Integer.SIZE;i++) {
 * int bit = c&(1<<i);
 * System.out.println(i+" :"+bit);
 * }
 * System.out.println("========================");
 * //b = b ^ (1<<1);
 * int a=0;
 * int pos = 12;
 * for (int i=0;i<=pos;i++) {
 * a = a + (b & (1<<i));
 * }
 * for (int i=pos+1;i<Integer.SIZE;i++) {
 * a = a + (c & (1<<i));
 * }
 * System.out.println(a);
 * for (int i=0;i<Integer.SIZE;i++) {
 * int bit = a&(1<<i);
 * System.out.println(i+" :"+bit);
 * }
 */