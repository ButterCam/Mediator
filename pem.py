with open("cert.pem") as r:
  text = r.read().replace("\n", "**").replace(" ", "%20")
with open("certm.pem", "w") as w:
  w.write(text)
