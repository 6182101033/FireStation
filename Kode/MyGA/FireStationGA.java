import java.util.Random;

// Mesin GA untuk penempatan Fire Station.
// Operator genetik dipilih lewat konfigurasi (representasi, selection, crossover, mutasi)
// sehingga 2 x 2 x 2 x 2 = 16 model Tahap 1 bisa dijalankan tanpa mengubah kode.
public class FireStationGA {
    // Pilihan setting
    public static final int REPR_BIT = 0;        // representasi 1: bit-string (panjang = jumlah sel kosong)
    public static final int REPR_FS = 1;         // representasi 2: array berisi p nomor sel kosong
    public static final int SEL_TOURNAMENT = 0;  // selection 1: tournament (ukuran 3)
    public static final int SEL_ROULETTE = 1;    // selection 2: roulette wheel
    public static final int CX_SINGLE = 0;       // crossover 1: single point
    public static final int CX_UNIFORM = 1;      // crossover 2: uniform
    public static final int MUT_A = 0;           // mutasi 1: BIT = swap (pindah 1 FS), FS = ganti 1 gen dengan sel kosong acak
    public static final int MUT_B = 1;           // mutasi 2: BIT = inversion, FS = geser 1 FS ke sel kosong bertetangga

    public static final String[] NAMA_REPR = {"Bit", "FSOnly"};                   // nama untuk CSV
    public static final String[] NAMA_SEL = {"Tournament", "Roulette"};           // nama untuk CSV
    public static final String[] NAMA_CX = {"SinglePoint", "Uniform"};            // nama untuk CSV

    // Nama mutasi bergantung representasi
    public static String namaMutasi(int representasi, int mutasi) {
        String[][] nama = {{"Swap", "Inversion"}, {"Replace", "Neighbor"}};       // [representasi][mutasi]
        return nama[representasi][mutasi];                                        // kembalikan nama
    }

    // Hyperparameter
    public int maxPopulationSize;   // ukuran populasi
    public int totalGeneration;     // jumlah generasi
    public double crossoverRate;    // peluang crossover (pc)
    public double mutationRate;     // peluang mutasi per anak (pm)
    public double elitismRate;      // persentase elitism

    // Operator
    public int representasi;        // REPR_BIT atau REPR_FS
    public int selection;           // SEL_TOURNAMENT atau SEL_ROULETTE
    public int crossover;           // CX_SINGLE atau CX_UNIFORM
    public int mutasi;              // MUT_A atau MUT_B

    // Konstruktor lengkap (dengan pilihan operator)
    public FireStationGA(int pop, int gen, double pc, double pm, double e,
                         int representasi, int selection, int crossover, int mutasi) {
        this.maxPopulationSize = pop;       // simpan ukuran populasi
        this.totalGeneration = gen;         // simpan jumlah generasi
        this.crossoverRate = pc;            // simpan pc
        this.mutationRate = pm;             // simpan pm
        this.elitismRate = e;               // simpan elitism
        this.representasi = representasi;   // simpan pilihan representasi
        this.selection = selection;         // simpan pilihan selection
        this.crossover = crossover;         // simpan pilihan crossover
        this.mutasi = mutasi;               // simpan pilihan mutasi
    }

    // Konstruktor lama: bit-string, tournament, single point, swap (perilaku awal)
    public FireStationGA(int pop, int gen, double pc, double pm, double e) {
        this(pop, gen, pc, pm, e, REPR_BIT, SEL_TOURNAMENT, CX_SINGLE, MUT_A);
    }

    // --- FUNGSI UTAMA EVOLUSI ---
    public Population evolve(Population currentPop, int panjangKromosom, int targetP, EvaluatorPeta evaluator, Random rand) {
        // 1. Elitism: individu terbaik langsung masuk generasi baru
        Population newPop = currentPop.getNewPopulationWElit();

        // 2. Isi populasi baru sampai penuh
        while (!newPop.isFilled()) {
            // --- PARENT SELECTION ---
            Individual[] parents = selectParents(currentPop, rand);       // pilih 2 induk sesuai konfigurasi

            // --- CROSSOVER ---
            Individual[] children;                                        // dua anak
            if (rand.nextDouble() < this.crossoverRate) {                 // crossover terjadi dengan peluang pc
                children = doCrossover(parents[0], parents[1], panjangKromosom, targetP, rand);
            } else {                                                      // tanpa crossover: anak = klon induk
                children = new Individual[]{parents[0].cloneIndividual(), parents[1].cloneIndividual()};
            }

            // --- MUTASI ---
            for (int i = 0; i < 2; i++) {
                if (rand.nextDouble() < this.mutationRate) {              // mutasi terjadi dengan peluang pm
                    doMutation(children[i], rand, evaluator, panjangKromosom);
                }
                if (!newPop.isFilled()) {                                 // masukkan anak jika masih ada slot
                    newPop.addIndividual(children[i]);
                }
            }
        }

        // 3. Hitung fitness semua individu generasi baru (sekaligus mengurutkan)
        newPop.computeAllFitnesses(evaluator, targetP);
        return newPop;
    }

    // ========================================================================
    // DISPATCH: memilih operator sesuai konfigurasi
    // ========================================================================

    // Memilih fungsi selection
    private Individual[] selectParents(Population pop, Random rand) {
        if (selection == SEL_ROULETTE) return selectParentRoulette(pop, rand);   // roulette wheel
        return selectParentTournament(pop, rand);                                // tournament (default)
    }

    // Memilih fungsi crossover sesuai representasi dan jenis crossover
    private Individual[] doCrossover(Individual p1, Individual p2, int panjang, int targetP, Random rand) {
        if (representasi == REPR_FS) {                                           // representasi array nomor sel
            if (crossover == CX_UNIFORM) return crossoverUniformFS(p1, p2, panjang, targetP, rand);
            return crossoverSinglePointFS(p1, p2, panjang, targetP, rand);
        }
        if (crossover == CX_UNIFORM) return crossoverUniform(p1, p2, panjang, targetP, rand);   // bit-string
        return crossoverSinglePoint(p1, p2, panjang, targetP, rand);
    }

    // Memilih fungsi mutasi sesuai representasi dan jenis mutasi
    private void doMutation(Individual ind, Random rand, EvaluatorPeta evaluator, int panjang) {
        if (representasi == REPR_FS) {                                           // representasi array nomor sel
            if (mutasi == MUT_B) mutateNeighborFS(ind, rand, evaluator, panjang);
            else mutateReplaceFS(ind, rand, panjang);
            return;
        }
        if (mutasi == MUT_B) mutateInversion(ind, rand);                         // bit-string
        else mutateSwap(ind, rand);
    }

    // ========================================================================
    // PARENT SELECTION
    // ========================================================================

    // 1A. Tournament: ambil 3 individu acak, yang fitness-nya terbesar jadi induk
    public Individual[] selectParentTournament(Population pop, Random rand) {
        Individual[] parents = new Individual[2];                                // dua induk
        for (int p = 0; p < 2; p++) {                                            // satu turnamen per induk
            Individual bestInTournament = null;                                  // pemenang sementara
            for (int i = 0; i < 3; i++) {                                        // 3 peserta
                int randomIdx = rand.nextInt(pop.listOfIndividual.size());       // undi peserta
                Individual kompetitor = pop.listOfIndividual.get(randomIdx);
                if (bestInTournament == null || kompetitor.getFitness() > bestInTournament.getFitness()) {
                    bestInTournament = kompetitor;                               // fitness lebih besar menang
                }
            }
            parents[p] = bestInTournament;                                       // pemenang jadi induk
        }
        return parents;
    }

    // 1B. Roulette wheel: peluang terpilih sebanding dengan fitness
    public Individual[] selectParentRoulette(Population pop, Random rand) {
        Individual[] parents = new Individual[2];                                // dua induk
        double sumFitness = 0;                                                   // total fitness populasi
        for (Individual ind : pop.listOfIndividual) sumFitness += ind.getFitness();

        for (int p = 0; p < 2; p++) {
            double prob = rand.nextDouble() * sumFitness;                        // posisi bola di roda
            double sum = 0;                                                      // akumulasi irisan
            int i;
            for (i = 0; i < pop.listOfIndividual.size(); i++) {
                sum += pop.listOfIndividual.get(i).getFitness();                 // tambah irisan individu i
                if (sum >= prob) break;                                          // bola jatuh di irisan ini
            }
            if (i >= pop.listOfIndividual.size()) i = pop.listOfIndividual.size() - 1; // jaga batas indeks
            parents[p] = pop.listOfIndividual.get(i);
        }
        return parents;
    }

    // ========================================================================
    // CROSSOVER - REPRESENTASI BIT-STRING
    // (anak bisa tidak valid: jumlah 1 != p; ditangani lewat fitness penalti)
    // ========================================================================

    // 2A. Single point
    public Individual[] crossoverSinglePoint(Individual p1, Individual p2, int panjangKromosom, int targetP, Random rand) {
        IndividualBit parent1 = (IndividualBit) p1;                              // induk 1
        IndividualBit parent2 = (IndividualBit) p2;                              // induk 2

        int[] kromosomAnak1 = new int[panjangKromosom];                          // gen anak 1
        int[] kromosomAnak2 = new int[panjangKromosom];                          // gen anak 2

        int titikPotong = rand.nextInt(panjangKromosom);                         // titik potong acak

        for (int i = 0; i < panjangKromosom; i++) {
            if (i < titikPotong) {                                               // sebelum titik potong: ikut induk sendiri
                kromosomAnak1[i] = parent1.chromosome[i];
                kromosomAnak2[i] = parent2.chromosome[i];
            } else {                                                             // setelah titik potong: tukar induk
                kromosomAnak1[i] = parent2.chromosome[i];
                kromosomAnak2[i] = parent1.chromosome[i];
            }
        }
        return new Individual[]{new IndividualBit(kromosomAnak1), new IndividualBit(kromosomAnak2)};
    }

    // 2B. Uniform
    public Individual[] crossoverUniform(Individual p1, Individual p2, int panjangKromosom, int targetP, Random rand) {
        IndividualBit parent1 = (IndividualBit) p1;                              // induk 1
        IndividualBit parent2 = (IndividualBit) p2;                              // induk 2

        int[] kromosomAnak1 = new int[panjangKromosom];                          // gen anak 1
        int[] kromosomAnak2 = new int[panjangKromosom];                          // gen anak 2

        for (int i = 0; i < panjangKromosom; i++) {
            if (rand.nextBoolean()) {                                            // lempar koin: gen i dari induk sendiri
                kromosomAnak1[i] = parent1.chromosome[i];
                kromosomAnak2[i] = parent2.chromosome[i];
            } else {                                                             // atau tukar
                kromosomAnak1[i] = parent2.chromosome[i];
                kromosomAnak2[i] = parent1.chromosome[i];
            }
        }
        return new Individual[]{new IndividualBit(kromosomAnak1), new IndividualBit(kromosomAnak2)};
    }

    // ========================================================================
    // CROSSOVER - REPRESENTASI ARRAY NOMOR SEL (FS-only)
    // Gen selalu terurut, jadi potongan awal = FS di bagian atas peta, potongan akhir = bagian bawah.
    // Gen kembar (dua FS di sel yang sama) diperbaiki dengan repair() sehingga anak selalu valid.
    // ========================================================================

    // 2A'. Single point
    public Individual[] crossoverSinglePointFS(Individual p1, Individual p2, int panjangKromosom, int targetP, Random rand) {
        int[] g1 = ((IndividualFSOnly) p1).genes;                                // gen induk 1
        int[] g2 = ((IndividualFSOnly) p2).genes;                                // gen induk 2
        int[] a = new int[targetP];                                              // gen anak 1
        int[] b = new int[targetP];                                              // gen anak 2
        int titikPotong = rand.nextInt(targetP);                                 // titik potong acak (0..p-1)
        for (int i = 0; i < targetP; i++) {
            if (i < titikPotong) { a[i] = g1[i]; b[i] = g2[i]; }                 // sebelum titik potong: induk sendiri
            else { a[i] = g2[i]; b[i] = g1[i]; }                                 // setelah titik potong: tukar
        }
        return buatAnakFS(a, b, panjangKromosom, rand);
    }

    // 2B'. Uniform
    public Individual[] crossoverUniformFS(Individual p1, Individual p2, int panjangKromosom, int targetP, Random rand) {
        int[] g1 = ((IndividualFSOnly) p1).genes;                                // gen induk 1
        int[] g2 = ((IndividualFSOnly) p2).genes;                                // gen induk 2
        int[] a = new int[targetP];                                              // gen anak 1
        int[] b = new int[targetP];                                              // gen anak 2
        for (int i = 0; i < targetP; i++) {
            if (rand.nextBoolean()) { a[i] = g1[i]; b[i] = g2[i]; }              // koin: induk sendiri
            else { a[i] = g2[i]; b[i] = g1[i]; }                                 // atau tukar
        }
        return buatAnakFS(a, b, panjangKromosom, rand);
    }

    // Membungkus gen menjadi individu lalu memperbaiki gen kembar
    private Individual[] buatAnakFS(int[] a, int[] b, int panjangKromosom, Random rand) {
        IndividualFSOnly anak1 = new IndividualFSOnly(a);                        // anak 1
        IndividualFSOnly anak2 = new IndividualFSOnly(b);                        // anak 2
        anak1.repair(panjangKromosom, rand);                                     // buang gen kembar anak 1
        anak2.repair(panjangKromosom, rand);                                     // buang gen kembar anak 2
        return new Individual[]{anak1, anak2};
    }

    // ========================================================================
    // MUTASI - REPRESENTASI BIT-STRING
    // ========================================================================

    // 3A. Swap: pindahkan 1 FS ke sel kosong lain
    public void mutateSwap(Individual ind, Random rand) {
        IndividualBit individu = (IndividualBit) ind;                            // bit-string
        int panjang = individu.chromosome.length;                                // panjang kromosom

        int jumlahSatu = 0;                                                      // hitung jumlah FS
        for (int v : individu.chromosome) jumlahSatu += v;
        if (jumlahSatu == 0 || jumlahSatu == panjang) return;                    // tidak ada yang bisa ditukar: hindari loop tak berujung

        int idxSatu = rand.nextInt(panjang);                                     // cari indeks berisi 1 (ada FS)
        while (individu.chromosome[idxSatu] != 1) {
            idxSatu = rand.nextInt(panjang);
        }

        int idxNol = rand.nextInt(panjang);                                      // cari indeks berisi 0 (sel tanpa FS)
        while (individu.chromosome[idxNol] != 0) {
            idxNol = rand.nextInt(panjang);
        }

        individu.chromosome[idxSatu] = 0;                                        // FS dipindahkan
        individu.chromosome[idxNol] = 1;
    }

    // 3B. Inversion: balik urutan gen pada satu segmen
    public void mutateInversion(Individual ind, Random rand) {
        IndividualBit individu = (IndividualBit) ind;                            // bit-string
        int panjang = individu.chromosome.length;                                // panjang kromosom

        int pt1 = rand.nextInt(panjang);                                         // dua titik acak
        int pt2 = rand.nextInt(panjang);

        int start = Math.min(pt1, pt2);                                          // batas kiri segmen
        int end = Math.max(pt1, pt2);                                            // batas kanan segmen

        while (start < end) {                                                    // balik segmen
            int temp = individu.chromosome[start];
            individu.chromosome[start] = individu.chromosome[end];
            individu.chromosome[end] = temp;
            start++;
            end--;
        }
    }

    // ========================================================================
    // MUTASI - REPRESENTASI ARRAY NOMOR SEL (FS-only)
    // ========================================================================

    // Cek apakah id sel sudah dipakai salah satu FS
    private boolean sudahDipakai(int[] genes, int id) {
        for (int g : genes) if (g == id) return true;                            // ketemu: sudah dipakai
        return false;                                                            // belum dipakai
    }

    // 3A'. Replace: ganti 1 FS dengan sel kosong acak yang belum dipakai
    public void mutateReplaceFS(Individual ind, Random rand, int panjangKromosom) {
        int[] genes = ((IndividualFSOnly) ind).genes;                            // gen individu
        if (panjangKromosom <= genes.length) return;                             // tidak ada sel kosong bebas
        int k = rand.nextInt(genes.length);                                      // FS mana yang dipindah
        int id = rand.nextInt(panjangKromosom);                                  // sel tujuan acak
        while (sudahDipakai(genes, id)) id = rand.nextInt(panjangKromosom);      // ulangi sampai belum dipakai
        genes[k] = id;                                                           // pindahkan FS
        java.util.Arrays.sort(genes);                                            // jaga gen tetap terurut
    }

    // 3B'. Neighbor: geser 1 FS ke sel kosong bertetangga (atas/bawah/kiri/kanan) -> perubahan kecil
    public void mutateNeighborFS(Individual ind, Random rand, EvaluatorPeta ev, int panjangKromosom) {
        int[] genes = ((IndividualFSOnly) ind).genes;                            // gen individu
        int k = rand.nextInt(genes.length);                                      // FS mana yang digeser
        EvaluatorPeta.Titik t = ev.daftarKosong.get(genes[k]);                   // posisi FS di grid
        int[] dr = {-1, 1, 0, 0};                                                // arah baris
        int[] dc = {0, 0, -1, 1};                                                // arah kolom
        int mulai = rand.nextInt(4);                                             // arah awal acak
        for (int d = 0; d < 4; d++) {                                            // coba keempat arah bergantian
            int arah = (mulai + d) % 4;
            int nr = t.r + dr[arah];                                             // baris tetangga
            int nc = t.c + dc[arah];                                             // kolom tetangga
            if (nr < 0 || nr >= ev.m || nc < 0 || nc >= ev.n) continue;          // di luar grid
            int id = ev.idSel[nr][nc];                                           // id sel tetangga (-1 jika rumah/pohon)
            if (id >= 0 && !sudahDipakai(genes, id)) {                           // sel kosong dan belum dipakai FS lain
                genes[k] = id;                                                   // geser FS
                java.util.Arrays.sort(genes);                                    // jaga gen tetap terurut
                return;
            }
        }
        mutateReplaceFS(ind, rand, panjangKromosom);                             // semua tetangga buntu: pindah ke sel acak
    }
}
