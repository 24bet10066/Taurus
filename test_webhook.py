import hmac
import hashlib
import json
import urllib.request
import urllib.parse

secret = b"test_webhook"
order_id = "order_CURL12345"
payment_id = "pay_CURL54321"

payload = {
  "event": "payment.captured",
  "payload": {
    "payment": {
      "entity": {
        "id": payment_id,
        "order_id": order_id,
        "amount": 15000,
        "status": "captured"
      }
    }
  }
}

raw_body = json.dumps(payload, separators=(',', ':'))
signature = hmac.new(secret, raw_body.encode('utf-8'), hashlib.sha256).hexdigest()

req = urllib.request.Request("http://localhost:8087/api/v1/payments/webhook/razorpay")
req.add_header("Content-Type", "application/json")
req.add_header("X-Razorpay-Signature", signature)
req.data = raw_body.encode('utf-8')

print(f"Triggering webhook for order {order_id}...")
try:
    with urllib.request.urlopen(req) as response:
        print("Response:", response.status)
except Exception as e:
    print("Error:", e)
