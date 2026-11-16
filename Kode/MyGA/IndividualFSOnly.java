import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

// Representasi kromosom ke-2: array berisi p nomor sel kosong yang ditempati Fire Station
public class IndividualFSOnly implements Individual {
    public int[] genes;      // genes[k] = id sel kosong (indeks di daftarKosong) tempat FS ke-k
    public double fitness;   // nilai fitness (makin besar makin baik)
    public double cost;      // rata-rata jarak rumah ke FS terdekat (makin kecil makin baik)

    // 1. Konstruktor individu acak untuk generasi 0
    public IndividualFSOnly(int panjangKromosom, int targetP, Random rand) {
        this.genes = new int[targetP];                       // panjang kromosom = p (bukan jumlah sel kosong)
        boolean[] dipakai = new boolean[panjangKromosom];    // penanda sel yang sudah dipilih agar tidak kembar
        int terisi = 0;                                      // jumlah gen yang sudah terisi
        while (terisi < targetP) {                           // ulangi sampai p gen terisi
            int id = rand.nextInt(panjangKromosom);          // pilih id sel kosong secara acak
            if (!dipakai[id]) {                              // hanya terima jika belum dipakai
                dipakai[id] = true;                          // tandai sudah dipakai
                this.genes[terisi++] = id;                   // simpan sebagai gen berikutnya
            }
        }
        Arrays.sort(this.genes);                             // urutkan gen (bentuk kanonik): {3,9} dan {9,3} dianggap sama
        this.fitness = 0;                                    // belum dihitung
        this.cost = Double.MAX_VALUE;                        // belum dihitung
    }

    // 2. Konstruktor untuk anak hasil crossover / kloning
    public IndividualFSOnly(int[] genesBaru) {
        this.genes = genesBaru.clone();                      // salin agar tidak berbagi array dengan induk
        Arrays.sort(this.genes);                             // urutkan gen (bentuk kanonik)
        this.fitness = 0;                                    // belum dihitung
        this.cost = Double.MAX_VALUE;                        // belum dihitung
    }

    // 3. Hitung cost dan fitness
    @Override
    public void calculateFitness(EvaluatorPeta evaluator, int targetP) {
        this.cost = evaluator.hitungCost(getFireStationIndices()); // kromosom selalu valid (jika sudah di-repair), langsung hitung cost
        if (this.cost == 0) {                                // hindari pembagian dengan nol
            this.fitness = 999999.0;                         // cost 0 = solusi sempurna
        } else {
            this.fitness = 1.0 / this.cost;                  // fitness berbanding terbalik dengan cost
        }
    }

    // 4. Perbaiki kromosom: ganti gen kembar dengan sel kosong acak yang belum terpakai
    public void repair(int panjangKromosom, Random rand) {
        boolean[] dipakai = new boolean[panjangKromosom];    // sel yang sudah dipakai gen sebelumnya
        for (int k = 0; k < genes.length; k++) {             // periksa tiap gen secara berurutan
            if (dipakai[genes[k]]) {                         // gen ini kembar dengan gen sebelumnya
                int id = rand.nextInt(panjangKromosom);      // cari pengganti secara acak
                while (dipakai[id] || isDipakaiGenLain(id, k)) { // ulangi jika pengganti bentrok
                    id = rand.nextInt(panjangKromosom);      // coba id lain
                }
                genes[k] = id;                               // pasang pengganti
            }
            dipakai[genes[k]] = true;                        // tandai gen ini sudah dipakai
        }
        Arrays.sort(genes);                                  // urutkan lagi setelah perbaikan
    }

    // Cek apakah id dipakai oleh gen selain indeks k (supaya pengganti tidak menabrak gen di depan)
    private boolean isDipakaiGenLain(int id, int k) {
        for (int j = k + 1; j < genes.length; j++) {         // gen setelah k belum ditandai, jadi dicek manual
            if (genes[j] == id) return true;                 // bentrok dengan gen yang akan datang
        }
        return false;                                        // aman
    }

    @Override
    public double getFitness() {
        return this.fitness;                                 // kembalikan fitness
    }

    @Override
    public double getCost() {
        return this.cost;                                    // kembalikan cost
    }

    // 5. Decode: genes sudah berupa id sel kosong, tinggal dibungkus menjadi List
    @Override
    public List<Integer> getFireStationIndices() {
        List<Integer> indices = new ArrayList<>();           // wadah hasil
        for (int g : genes) indices.add(g);                  // salin tiap gen
        return indices;                                      // kembalikan daftar id FS
    }

    @Override
    public Individual cloneIndividual() {
        IndividualFSOnly kloning = new IndividualFSOnly(this.genes); // salin gen
        kloning.fitness = this.fitness;                      // salin fitness agar tidak perlu hitung ulang
        kloning.cost = this.cost;                            // salin cost
        return kloning;                                      // kembalikan klon
    }

    // 6. Urutkan dari fitness terbesar ke terkecil
    @Override
    public int compareTo(Individual other) {
        return Double.compare(other.getFitness(), this.getFitness()); // descending
    }

    // 7. Main untuk mengetes kelas ini sendiri
    // Cara run (dari folder FireStation 2): java -cp Kode/MyGA IndividualFSOnly [file peta] [seed]
    public static void main(String[] args) {
        String filePeta = (args.length > 0) ? args[0] : "TestCases/n20_acak_01.txt"; // peta default
        long seed = (args.length > 1) ? Long.parseLong(args[1]) : 1;                 // seed default
        Random rand = new Random(seed);                                              // RNG untuk tes

        EvaluatorPeta ev = new EvaluatorPeta();                                      // siapkan evaluator
        ev.muatPeta(filePeta);                                                       // baca peta
        ev.hitungMatriksJarak();                                                     // pre-komputasi BFS
        int panjang = ev.daftarKosong.size();                                        // jumlah sel kosong
        int p = ev.p;                                                                // jumlah FS

        // Tes 1: individu acak, hitung cost, tampilkan dengan format output soal
        IndividualFSOnly ind = new IndividualFSOnly(panjang, p, rand);               // buat individu acak
        ind.calculateFitness(ev, p);                                                 // hitung cost dan fitness
        System.out.println("--- Tes 1: individu acak ---");
        System.out.println(p + " " + String.format(java.util.Locale.US, "%.5f", ind.getCost())); // baris 1: p dan cost
        for (int id : ind.getFireStationIndices()) {                                 // tiap FS
            EvaluatorPeta.Titik t = ev.daftarKosong.get(id);                         // id -> posisi grid
            System.out.println((t.c + 1) + " " + (t.r + 1));                         // "x y" (kolom, baris), (1,1) kiri atas
        }
        System.out.println("Fitness = " + ind.getFitness());                         // fitness = 1/cost

        // Tes 2: semua gen harus berbeda
        java.util.Set<Integer> unik = new java.util.HashSet<>(ind.getFireStationIndices()); // buang duplikat
        System.out.println("--- Tes 2: gen berbeda? " + (unik.size() == p ? "OK" : "GAGAL")); // harus p unik

        // Tes 3: repair harus menghilangkan gen kembar
        int[] kembar = new int[p];                                                   // kromosom sengaja kembar semua
        java.util.Arrays.fill(kembar, 0);                                            // semua gen = sel #0
        IndividualFSOnly rusak = new IndividualFSOnly(kembar);                       // bungkus jadi individu
        rusak.repair(panjang, rand);                                                 // perbaiki
        java.util.Set<Integer> unik2 = new java.util.HashSet<>(rusak.getFireStationIndices()); // cek keunikan
        System.out.println("--- Tes 3: repair? " + (unik2.size() == p ? "OK" : "GAGAL")); // harus p unik

        // Tes 4: klon harus sama tapi tidak berbagi array
        IndividualFSOnly klon = (IndividualFSOnly) ind.cloneIndividual();            // klon individu
        klon.genes[0] = (klon.genes[0] + 1) % panjang;                               // ubah klon
        System.out.println("--- Tes 4: klon independen? " + (klon.genes[0] != ind.genes[0] ? "OK" : "GAGAL")); // asli tak berubah
    }
}
