import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class EvaluatorPeta {
    
    // Kelas sederhana untuk menyimpan koordinat sel di grid
    public static class Titik {
        public int r, c;
        public Titik(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }

    public int m, n, p; // Dimensi grid (m x n) dan jumlah Fire Station yang harus dipasang (p)
    public char[][] grid;
    public List<Titik> daftarRumah = new ArrayList<>();
    public List<Titik> daftarKosong = new ArrayList<>();
    
    // Matriks Jarak: matrixJarak[id_rumah][id_sel_kosong]
    public int[][] matrixJarak;
    public int penaltyP; // Penalti P = m * n jika sel tidak tercapai

    // 1. Membaca file masukan peta (.txt)
    public void muatPeta(String filepath) {
        daftarRumah.clear();
        daftarKosong.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            // Baca baris pertama: m n p
            String[] header = br.readLine().trim().split("\\s+");
            m = Integer.parseInt(header[0]);
            n = Integer.parseInt(header[1]);
            p = Integer.parseInt(header[2]);
            penaltyP = m * n; // Nilai penalti sesuai spesifikasi soal

            grid = new char[m][n];

            // Baca m baris berikutnya
            for (int r = 0; r < m; r++) {
                String baris = br.readLine().trim().replace(" ", "");
                for (int c = 0; c < n; c++) {
                    char sel = baris.charAt(c);
                    grid[r][c] = sel;

                    if (sel == 'H' || sel == 'h') {
                        daftarRumah.add(new Titik(r, c));
                    } else if (sel == '.') {
                        daftarKosong.add(new Titik(r, c));
                    }
                }
            }
            System.out.println("Peta Berhasil Dimuat!");
            System.out.println("Ukuran Grid: " + m + "x" + n + " | Target FS (p): " + p);
            System.out.println("Jumlah Rumah: " + daftarRumah.size() + " | Jumlah Sel Kosong: " + daftarKosong.size());

        } catch (IOException e) {
            System.err.println("Gagal membaca file: " + e.getMessage());
        }
    }

    // 2. Pre-Komputasi BFS untuk membangun Matriks Jarak
    public void hitungMatriksJarak() {
        int totalRumah = daftarRumah.size();
        int totalKosong = daftarKosong.size();
        
        matrixJarak = new int[totalRumah][totalKosong];

        // Arah pergerakan 4 arah (Atas, Bawah, Kiri, Kanan)
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        // Lakukan BFS untuk setiap rumah
        for (int i = 0; i < totalRumah; i++) {
            Titik rumah = daftarRumah.get(i);
            
            // Matriks untuk mencatat jarak BFS dari rumah ke setiap sel di grid
            int[][] dist = new int[m][n];
            for (int r = 0; r < m; r++) {
                for (int c = 0; c < n; c++) {
                    dist[r][c] = -1; // -1 artinya belum dikunjungi
                }
            }

            Queue<Titik> queue = new LinkedList<>();
            queue.add(rumah);
            dist[rumah.r][rumah.c] = 0;

            // Proses BFS
            while (!queue.isEmpty()) {
                Titik curr = queue.poll();

                for (int d = 0; d < 4; d++) {
                    int nr = curr.r + dr[d];
                    int nc = curr.c + dc[d];

                    // Cek apakah koordinat tetangga valid (di dalam batas grid)
                    if (nr >= 0 && nr < m && nc >= 0 && nc < n) {
                        // Jalur HANYA boleh melewati sel kosong ('.')
                        // Rumah lain ('H') dan Pohon ('T') adalah rintangan yang tidak bisa dilewati
                        if (grid[nr][nc] == '.' && dist[nr][nc] == -1) {
                            dist[nr][nc] = dist[curr.r][curr.c] + 1;
                            queue.add(new Titik(nr, nc));
                        }
                    }
                }
            }

            // Simpan hasil jarak dari rumah ke semua sel kosong ke matriks
            for (int j = 0; j < totalKosong; j++) {
                Titik selKosong = daftarKosong.get(j);
                int d = dist[selKosong.r][selKosong.c];

                if (d != -1) {
                    matrixJarak[i][j] = d;
                } else {
                    // Jika sel tidak dapat dicapai, berikan jarak Penalti P = m * n
                    matrixJarak[i][j] = penaltyP;
                }
            }
        }
        System.out.println("Pre-komputasi BFS Matriks Jarak Selesai!");
    }

    // 3. Fungsi untuk menghitung total Rata-Rata Cost dari kombinasi FS terpilih
    // Parameter: daftar ID sel kosong yang ditempati Fire Station
    public double hitungCost(List<Integer> indeksFSTerpilih) {
        int totalRumah = daftarRumah.size();
        double totalJarakMinimum = 0;

        // Untuk setiap rumah, cari FS terdekat dari daftar FS terpilih
        for (int i = 0; i < totalRumah; i++) {
            int jarakTerdekat = Integer.MAX_VALUE;

            for (int idFS : indeksFSTerpilih) {
                int d = matrixJarak[i][idFS];
                if (d < jarakTerdekat) {
                    jarakTerdekat = d;
                }
            }
            totalJarakMinimum += jarakTerdekat;
        }

        // Hitung rata-rata cost seluruh rumah ke FS terdekatnya
        return totalJarakMinimum / totalRumah;
    }
}