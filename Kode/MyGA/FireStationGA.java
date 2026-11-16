import java.util.Random;

public class FireStationGA {
    public int maxPopulationSize;
    public int totalGeneration;
    public double crossoverRate;
    public double mutationRate;
    public double elitismRate;

    public FireStationGA(int pop, int gen, double pc, double pm, double e) {
        this.maxPopulationSize = pop;
        this.totalGeneration = gen;
        this.crossoverRate = pc;
        this.mutationRate = pm;
        this.elitismRate = e;
    }

    // --- FUNGSI UTAMA EVOLUSI ---
    public Population evolve(Population currentPop, int panjangKromosom, int targetP, EvaluatorPeta evaluator, Random rand) {
        // 1. Elitism: Langsung masukkan individu terbaik ke generasi baru
        Population newPop = currentPop.getNewPopulationWElit();

        // 2. Looping sampai populasi baru penuh
        while (!newPop.isFilled()) {
            
            // --- PARENT SELECTION ---
            // Silakan ganti menjadi selectParentRoulette(currentPop, rand) untuk mencoba opsi lain
            Individual[] parents = selectParentTournament(currentPop, rand);
            Individual parent1 = parents[0];
            Individual parent2 = parents[1];

            // --- CROSSOVER ---
            Individual[] children = new Individual[2];
            if (rand.nextDouble() < this.crossoverRate) {
                // Silakan ganti menjadi crossoverUniform(...) untuk mencoba opsi lain
                children = crossoverSinglePoint(parent1, parent2, panjangKromosom, targetP, rand);
            } else {
                children[0] = parent1.cloneIndividual();
                children[1] = parent2.cloneIndividual();
            }

            // --- MUTASI ---
            for (int i = 0; i < 2; i++) {
                if (rand.nextDouble() < this.mutationRate) {
                    // Silakan ganti menjadi mutateInversion(...) untuk mencoba opsi lain
                    mutateSwap(children[i], rand);
                }
                
                // Tambahkan anak ke populasi baru (jika masih ada slot)
                if (!newPop.isFilled()) {
                    newPop.addIndividual(children[i]);
                }
            }
        }

        // 3. Hitung fitness untuk semua individu di generasi baru ini
        newPop.computeAllFitnesses(evaluator, targetP);
        return newPop;
    }

    // ========================================================================
    // KUMPULAN OPERATOR GENETIK (BISA DIGANTI-GANTI UNTUK EKSPERIMEN TAHAP 1)
    // ========================================================================

    // 1A. Parent Selection: Tournament (Rekomendasi)
    public Individual[] selectParentTournament(Population pop, Random rand) {
        Individual[] parents = new Individual[2];
        for (int p = 0; p < 2; p++) {
            // Ambil 3 individu acak untuk diadu
            Individual bestInTournament = null;
            for (int i = 0; i < 3; i++) {
                int randomIdx = rand.nextInt(pop.listOfIndividual.size());
                Individual kompetitor = pop.listOfIndividual.get(randomIdx);
                if (bestInTournament == null || kompetitor.getFitness() > bestInTournament.getFitness()) {
                    bestInTournament = kompetitor;
                }
            }
            parents[p] = bestInTournament;
        }
        return parents;
    }

    // 1B. Parent Selection: Roulette Wheel (Dari kode asli Anda)
    public Individual[] selectParentRoulette(Population pop, Random rand) {
        Individual[] parents = new Individual[2];
        double sumFitness = 0;
        for (Individual ind : pop.listOfIndividual) sumFitness += ind.getFitness();
        
        for (int p = 0; p < 2; p++) {
            double prob = rand.nextDouble() * sumFitness;
            double sum = 0;
            int i = 0;
            for (i = 0; i < pop.listOfIndividual.size(); i++) {
                sum += pop.listOfIndividual.get(i).getFitness();
                if (sum >= prob) break;
            }
            // Mencegah index out of bounds
            if(i >= pop.listOfIndividual.size()) i = pop.listOfIndividual.size() - 1; 
            parents[p] = pop.listOfIndividual.get(i);
        }
        return parents;
    }

    // 2A. Crossover: Single Point
    public Individual[] crossoverSinglePoint(Individual p1, Individual p2, int panjangKromosom, int targetP, Random rand) {
        IndividualBit parent1 = (IndividualBit) p1;
        IndividualBit parent2 = (IndividualBit) p2;
        
        int[] kromosomAnak1 = new int[panjangKromosom];
        int[] kromosomAnak2 = new int[panjangKromosom];
        
        int titikPotong = rand.nextInt(panjangKromosom);
        
        for (int i = 0; i < panjangKromosom; i++) {
            if (i < titikPotong) {
                kromosomAnak1[i] = parent1.chromosome[i];
                kromosomAnak2[i] = parent2.chromosome[i];
            } else {
                kromosomAnak1[i] = parent2.chromosome[i];
                kromosomAnak2[i] = parent1.chromosome[i];
            }
        }
        return new Individual[]{new IndividualBit(kromosomAnak1), new IndividualBit(kromosomAnak2)};
    }

    // 2B. Crossover: Uniform
    public Individual[] crossoverUniform(Individual p1, Individual p2, int panjangKromosom, int targetP, Random rand) {
        IndividualBit parent1 = (IndividualBit) p1;
        IndividualBit parent2 = (IndividualBit) p2;
        
        int[] kromosomAnak1 = new int[panjangKromosom];
        int[] kromosomAnak2 = new int[panjangKromosom];
        
        for (int i = 0; i < panjangKromosom; i++) {
            if (rand.nextBoolean()) {
                kromosomAnak1[i] = parent1.chromosome[i];
                kromosomAnak2[i] = parent2.chromosome[i];
            } else {
                kromosomAnak1[i] = parent2.chromosome[i];
                kromosomAnak2[i] = parent1.chromosome[i];
            }
        }
        return new Individual[]{new IndividualBit(kromosomAnak1), new IndividualBit(kromosomAnak2)};
    }

    // 3A. Mutasi: Swap (Memindahkan 1 FS ke kotak kosong lain)
    public void mutateSwap(Individual ind, Random rand) {
        IndividualBit individu = (IndividualBit) ind;
        int panjang = individu.chromosome.length;
        
        // Cari satu indeks yang berisi 1 (Ada FS)
        int idxSatu = rand.nextInt(panjang);
        while (individu.chromosome[idxSatu] != 1) {
            idxSatu = rand.nextInt(panjang);
        }
        
        // Cari satu indeks yang berisi 0 (Sel Kosong)
        int idxNol = rand.nextInt(panjang);
        while (individu.chromosome[idxNol] != 0) {
            idxNol = rand.nextInt(panjang);
        }
        
        // Tukar (Pindahkan FS)
        individu.chromosome[idxSatu] = 0;
        individu.chromosome[idxNol] = 1;
    }

    // 3B. Mutasi: Inversion (Membalik urutan segment gen)
    public void mutateInversion(Individual ind, Random rand) {
        IndividualBit individu = (IndividualBit) ind;
        int panjang = individu.chromosome.length;
        
        int pt1 = rand.nextInt(panjang);
        int pt2 = rand.nextInt(panjang);
        
        int start = Math.min(pt1, pt2);
        int end = Math.max(pt1, pt2);
        
        // Balik array dari start sampai end
        while (start < end) {
            int temp = individu.chromosome[start];
            individu.chromosome[start] = individu.chromosome[end];
            individu.chromosome[end] = temp;
            start++;
            end--;
        }
    }

    
    

    
}