reset
set terminal postscript eps enhanced color font 'Helvetica,10'
set output 'introduction.eps'
set output "f.eps"
set size ratio 0.2
set xlabel "Pos Tag"
set ylabel "%"
set xtics nomirror rotate by -45 font ",8"
set grid
set boxwidth 0.60 relative
set style fill transparent solid 0.5 noborder
plot "prf" every ::20::29 u 4:xtic(1) w boxes  title "f" 
