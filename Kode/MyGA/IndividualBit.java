import java.util.ArrayList;
import java.util.List;
import java.util.Random;
//representasi kromosom: semua sel kosong yang ditandai diisi/tidak
public class IndividualBit implements Individual {
    public int[] chromosome;
    public double fitness;
    public double cost;
    public double parentProbability; // Probabilitas untuk Roulette Wheel Selection
    
    // 1. Constructor untuk membuat Individu Acak Awal (Generasi 0)
    public IndividualBit(int panjangKromosom, int targetP, Random rand) {
        this.chromosome = new int[panjangKromosom];
        
        // Trik cerdas: Daripada mengacak 0 dan 1 secara liar yang pasti melanggar aturan,
        // kita paksa individu generasi pertama memiliki tepat 'p' buah angka 1 (Fire Station).
        int terisi = 0;
        while (terisi < targetP) {
            int indeksAcak = rand.nextInt(panjangKromosom);
            if (this.chromosome[indeksAcak] == 0) {
                this.chromosome[indeksAcak] = 1;
                terisi++;
            }
        }
        this.fitness = 0;
        this.cost = Double.MAX_VALUE;
    }
    
    // 2. Constructor untuk Individu hasil Crossover / Kloning
    public IndividualBit(int[] kromosomBaru) {
        this.chromosome = kromosomBaru.clone();
        this.fitness = 0;
        this.cost = Double.MAX_VALUE;
    }

    // 3. Inti Logika: Menghitung Fitness dan Menangani Kromosom Cacat
    @Override
    public void calculateFitness(EvaluatorPeta evaluator, int targetP) {
        List<Integer> fsTerpilih = getFireStationIndices();
        
        // SISTEM PENALTI: Menjawab Langkah 3.d di Soal Tugas Anda.
        // Jika hasil Crossover atau Mutasi membuat jumlah Fire Station tidak sama dengan p
        if (fsTerpilih.size() != targetP) {
            this.cost = Double.MAX_VALUE; 
            this.fitness = 0.00000001; // Fitness dihukum mendekati nol (akan mati seleksi alam)
        } else {
            // Jika kromosom valid (jumlah FS pas = p), minta EvaluatorPeta menghitung jarak
            this.cost = evaluator.hitungCost(fsTerpilih);
            
            // Fitness berbanding terbalik dengan cost. (Makin kecil jarak, makin besar fitness)
            if (this.cost == 0) {
                this.fitness = 999999.0; // Menghindari program error dibagi nol
            } else {
                this.fitness = 1.0 / this.cost; 
            }
        }
    }

    @Override
    public double getFitness() {
        return this.fitness;
    }

    @Override
    public double getCost() {
        return this.cost;
    }

    // 4. Menerjemahkan deretan Bit-String menjadi daftar ID sel kosong
    @Override
    public List<Integer> getFireStationIndices() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < chromosome.length; i++) {
            if (chromosome[i] == 1) { // 1 Berarti ada Fire Station
                indices.add(i);
            }
        }
        return indices;
    }

    @Override
    public Individual cloneIndividual() {
        IndividualBit kloning = new IndividualBit(this.chromosome);
        kloning.fitness = this.fitness;
        kloning.cost = this.cost;
        return kloning;
    }

    // 5. Fungsi untuk membandingkan 2 individu (dipakai saat Tournament atau Elitism)
    @Override
    public int compareTo(Individual other) {
        // Mengurutkan dari fitness TERBESAR ke TERKECIL
        return Double.compare(other.getFitness(), this.getFitness());
    }
}