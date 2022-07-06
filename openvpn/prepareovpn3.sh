export O3=$PWD/src/main/cpp/openvpn3
mkdir -p src/ovpn3/java/net/openvpn/ovpn3
swig -outdir src/ovpn3/java/net/openvpn/ovpn3/ -c++ -java -package net.openvpn.ovpn3 -I$O3/client -I$O3 $O3/javacli/ovpncli.i