# Fuente del diagrama de despliegue

- `python3 generar_diagrama.py` reescribe `../diagrama-despliegue.drawio` (logos de `icons/`, Simple Icons CC0).
- Exportar a PNG/PDF en el PC (Docker): copiar el `.drawio` a `pceduardo` y correr
  `docker run --rm -v "$PWD":/data rlespinasse/drawio-export -f png --scale 2 -o out2` (y `-f pdf`); usar un directorio de salida nuevo (los de antes quedan con dueño root).
