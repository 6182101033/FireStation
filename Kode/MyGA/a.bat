@echo off
rem Compile semua source GA Fire Station (jalankan dari folder ini)
javac *.java
rem Cara run (dari folder induk yang berisi TestCases/, mis. folder FireStation):
rem   cd ..\..
rem   java -cp Kode\MyGA Main
rem   java -cp Kode\MyGA IndividualFSOnly
rem Eksperimen Tahap 1 (16 model x 12 peta x 30 seed = 5760 run), hasil ke Hasil_Tahap1_16model.csv:
rem   java -cp Kode\MyGA EksperimenTahap1
rem   java -cp Kode\MyGA EksperimenTahap1 model=1-8 out=bagian1.csv threads=4
rem   java -cp Kode\MyGA EksperimenTahap1 peta=n20 iter=3 out=uji.csv
