ffmpeg -f v4l2 -i '/dev/video0' -r 6 -f alsa -i default -acodec libmp3lame -ar 22050 -f flv 'rtmp://127.0.0.1:1935/live/mystream' &
python3 webauth.py &
python3 webstreaming.py &