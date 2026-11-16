import java.util.List;

// Menggunakan Interface agar kita bisa membuat berbagai jenis kromosom (Bit-String, Array Int, dll)
public interface Individual extends Comparable<Individual> {
    
    // Fungsi untuk menghitung fitness dengan bantuan EvaluatorPeta
    void calculateFitness(EvaluatorPeta evaluator, int targetP);
    
    // Mengambil nilai fitness (Semakin tinggi semakin baik)
    double getFitness();
    
    // Mengambil nilai rata-rata jarak sebenarnya (Cost)
    double getCost();
    
    // Menerjemahkan kromosom menjadi daftar ID sel kosong yang terpilih
    List<Integer> getFireStationIndices();
    
    // Melakukan kloning (sangat penting untuk proses Elitism di GA)
    Individual cloneIndividual();
}