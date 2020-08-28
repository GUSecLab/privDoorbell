python3 -c "import random
print(random.getrandbits(128))" > seed.conf
cat /var/lib/tor/hidden_service/hostname > hostname.conf
python3 generate_config.py
rm seed.conf
rm hostname.conf