# EGM96 15-minute geoid model

- Asset: `us_nga_egm96_15.tif` (2,710,815 bytes).
- Downloaded: 2026-11-18 from https://cdn.proj.org/us_nga_egm96_15.tif
- Publisher: PROJ CDN; source model: US National Geospatial-Intelligence Agency, EGM96.
- Source and license statement: https://cdn.proj.org/us_nga_README.txt — **Public Domain**.
- PROJ describes the asset as a GeoTIFF converted from GTX, a 15-minute worldwide geoid undulation grid transforming physical heights to WGS84 ellipsoidal heights. TIFF metadata dates the source GTX to 2018-10-05; CDN lists publication as 2020-01-24.
- SHA-256: `db493027562c9b004d7220fa881f5603adada4e1c5029b933fa7de4547b0e78d`.
- Grid: 1440 × 721, PixelIsPoint, first sample (-180°, 90°), spacing (0.25°, -0.25°), longitude periodic; values in metres.
- Formula: `h = H + N`, where h is WGS84 ellipsoid height, H is EGM96 orthometric height and N is the grid value. This application uses bilinear interpolation and writes `H = h - N` to the template, retaining h in execution.
- GeographicLib documents EGM96-15 bilinear interpolation error against the underlying model as max 1.152 m / RMS 40 mm globally; this is not the accuracy of equipment coordinates, terrain or original survey heights. Local regression points use a 0.15 m tolerance against GeographicLib's higher-resolution online evaluation.
- References: https://geographiclib.sourceforge.io/1.52/geoid.html ; https://developer.dji.com/doc/cloud-api-tutorial/en/api-reference/dji-wpml/template-kml.html

This is a geoid model, **not** a terrain elevation raster. No input coordinates are sent to the publisher at runtime; the application fetches this static local asset on demand. Integrity failure blocks conversion without a zero-offset fallback.
