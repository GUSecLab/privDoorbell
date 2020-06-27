python -c "import random
print(random.getrandbits(128))" > seed.conf
cat /var/lib/tor/hidden_service/hostname > hostname.conf