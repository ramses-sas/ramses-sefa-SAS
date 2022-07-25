from datetime import datetime
from http.server import BaseHTTPRequestHandler, HTTPServer
from time import sleep
import os
import json

bind_ip = '0.0.0.0' #localhost
env_port = os.environ.get('PORT')
bind_port = int(env_port) if env_port else 48096
recv_buff = 1024*128

class handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if "favicon" not in self.path:
            print("[",datetime.now().strftime("%d/%m/%Y %H:%M:%S"),"] - GET: ",self.path)
        self.send_response(200, "OK")
        self.end_headers()
        self.wfile.write(bytes("OK", "ascii"))
    
    def do_POST(self):
        print("[",datetime.now().strftime("%d/%m/%Y %H:%M:%S"),"] - POST: ",self.path)
        content_len = int(self.headers.get('Content-Length'))
        post_body = self.rfile.read(content_len).decode('ascii')
        print("---- Start of body ----")
        print("%s" % post_body)
        print("---- End of body ----")
        post_body = json.loads(post_body)
        address = post_body["address"]
        number = int(post_body["number"])
        if number == 0:
            self.send_response(500, "Server Error")
            self.end_headers()
            self.wfile.write(bytes("Forcing server error", "ascii"))
        else:
            sleep(number/10)
            self.send_response(200, "OK")
            self.end_headers()
            self.wfile.write(bytes("Order delivered to "+address+" from service 2 after waiting "+str(number/10)+" seconds", "ascii"))
        #self.send_header('Content-type','text/html')ù
        # Response body
        # message = "Hello, World!"
        # self.wfile.write(bytes(message, "ascii"))
    
    def log_message(self, format, *args):
        return

with HTTPServer((bind_ip, bind_port), handler) as server:
    print("Server started")
    server.serve_forever()

