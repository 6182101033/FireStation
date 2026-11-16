import java.io.File;
import java.io.PrintWriter;
import java.util.Random;

public class EksperimenTahap1 {
    public static void main(String[] args) {
        // 1. Setup Seed Berdasarkan NPM Ketua
        int npm_ketua = 21033; // UBAH dengan NPM ketua kelompokmu
        Random rand = new Random(npm_ketua);
        int[] seed = new int[30];
        int batas = 1 << 16;
        for (int i = 0; i < seed.length; i++) {
            seed[i] = rand.nextInt(batas + 1);
        }

        // 2. Setup 12 File Peta
        String[] daftarPeta = {
            "TestCases/n20_acak_01.txt", "TestCases/n20_acak_02.txt",
            "TestCases/n20_klaster_01.txt", "TestCases/n20_labirin_01.txt",

            "TestCases/n40_acak_01.txt", "TestCases/n40_acak_02.txt",
            "TestCases/n40_klaster_01.txt", "TestCases/n40_labirin_01.txt",

            "TestCases/n60_acak_01.txt", "TestCases/n60_acak_02.txt",
            "TestCases/n60_klaster_01.txt", "TestCases/n60_labirin_01.txt"
        };

        // 3. Hyper-parameter Tetap Tahap 1
        int pop = 100;
        double pc = 0.8;
        double pm = 0.05;
        double elitism = 0.20;

        try {
            PrintWriter csvWriter = new PrintWriter(new File("Hasil_Tahap1.csv"));
            csvWriter.println("Peta,Iterasi,Seed,Evaluasi_Budget,Generasi,Cost_Terbaik,Waktu_ms");

            int totalRun = 0;

            // LOOP 1: tiap peta
            for (String peta : daftarPeta) {

                EvaluatorPeta ev = new EvaluatorPeta();
                ev.muatPeta(peta);
                ev.hitungMatriksJarak();

                int evalBudget = 10000;
                if (peta.contains("n40")) evalBudget = 50000;
                if (peta.contains("n60")) evalBudget = 200000;

                int maxGen = evalBudget / pop;
                int p = ev.p;
                int panjangKromosom = ev.daftarKosong.size();

                // LOOP 2: 30x percobaan
                for (int iterasi = 0; iterasi < 30; iterasi++) {

                    long startTime = System.currentTimeMillis();

                    Random gen = new Random(seed[iterasi]);
                    FireStationGA ga = new FireStationGA(pop, maxGen, pc, pm, elitism);
                    Population population = new Population(pop, panjangKromosom, p, elitism, ev, gen);

                    for (int genIdx = 1; genIdx <= maxGen; genIdx++) {
                        population = ga.evolve(population, panjangKromosom, p, ev, gen);
                    }

                    double cost = population.getBestIndividual().getCost();
                    long endTime = System.currentTimeMillis();

                    csvWriter.println(peta + "," + (iterasi + 1) + "," + seed[iterasi] + "," +
                                      evalBudget + "," + maxGen + "," + cost + "," + (endTime - startTime));
                    totalRun++;
                }
                System.out.println("Selesai memproses peta: " + peta);
                csvWriter.flush();
            }
            csvWriter.close();
            System.out.println("Eksperimen Tahap 1 Selesai! Total run: " + totalRun);

        } catch (Exception e) {
            System.out.println("Terjadi kesalahan saat menulis CSV!");
            e.printStackTrace();
        }
    }
}