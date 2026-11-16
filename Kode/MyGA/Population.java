import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Population {
    public List<Individual> listOfIndividual;
    private int maxPopulationSize;
    public double elitismPct;
    public Random MyRand;

    // 1. Constructor untuk membuat Populasi Awal (Generasi 0)
    public Population(int maxPopulationSize, int panjangKromosom, int targetP, double elitismPct, EvaluatorPeta evaluator, Random MyRand) {
        this.MyRand = MyRand;
        this.maxPopulationSize = maxPopulationSize;
        this.elitismPct = elitismPct;
        this.listOfIndividual = new ArrayList<>();

        for (int i = 0; i < maxPopulationSize; i++) {
            Individual ind = new IndividualBit(panjangKromosom, targetP, this.MyRand);
            this.listOfIndividual.add(ind);
        }
        
        // Memakai ulang logika dari computeAllFitnesses() di kode lama Anda
        computeAllFitnesses(evaluator, targetP);
    }

    // 2. Constructor kosong untuk menampung Generasi Baru (Keturunan)
    public Population(int maxPopulationSize, double elitismPct, Random MyRand) {
        this.MyRand = MyRand;
        this.maxPopulationSize = maxPopulationSize;
        this.elitismPct = elitismPct;
        this.listOfIndividual = new ArrayList<>();
    }

    // 3. Menghitung fitness semua individu lalu mengurutkannya (Mirip kode lama Anda)
    public void computeAllFitnesses(EvaluatorPeta evaluator, int targetP) {
        for (Individual ind : listOfIndividual) {
            ind.calculateFitness(evaluator, targetP);
        }
        // Urutkan dari yang fitness-nya paling besar ke terkecil
        Collections.sort(this.listOfIndividual);
    }

    // 4. Memasukkan individu baru ke populasi (Adaptasi dari kode lama)
    public boolean addIndividual(Individual newIdv) {
        if (this.listOfIndividual.size() >= this.maxPopulationSize) {
            return false; // Populasi penuh
        }
        this.listOfIndividual.add(newIdv);
        return true;
    }

    // 5. Mengecek apakah populasi sudah penuh (Dari kode lama)
    public boolean isFilled() {
        return this.listOfIndividual.size() == this.maxPopulationSize;
    }

    // 6. Mengambil individu terbaik (Mirip getBestIdv dari kode lama)
    public Individual getBestIndividual() {
        if (listOfIndividual.isEmpty()) return null;
        // Karena sudah disortir, indeks ke-0 selalu yang terbaik
        return this.listOfIndividual.get(0).cloneIndividual(); 
    }

    // 7. Proses Elitism (Adaptasi fungsi getNewPopulationWElit dari kode lama)
    public Population getNewPopulationWElit() {
        Population newPop = new Population(this.maxPopulationSize, this.elitismPct, this.MyRand);
        
        int n = (int)(this.elitismPct * this.maxPopulationSize);
        // Memasukkan n individu terbaik ke populasi baru secara langsung
        for (int i = 0; i < n; i++) {
            newPop.addIndividual(this.listOfIndividual.get(i).cloneIndividual());
        }
        return newPop;
    }
}