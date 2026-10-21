# Banco visual: fuentes y condiciones

## Reproducción y límites

Ejecutar el workflow `Frozen visual bank` de `.github/workflows/visual-bank.yml`. `tools/stage_visual_bank.py` descarga y comprueba cada entrada; `VisualBankTest` prepara el módulo de Google Play services, ejecuta `PhotoContours.analyze` y `extract` sin cambiar producción, y renderiza con `ExportRenderer` a progreso 1.0. El resultado incluye disponibilidad del módulo, tipo de detección, caras, trazos y tiempo. Un resultado vacío también se conserva. La suite normal omite este recolector cuando no se han preparado sus 30 assets.

La disponibilidad del modelo de sujeto y el código de la aplicación se registran por separado: los pesos que Google Play services entrega no se fijan por un SHA del repositorio. Los tiempos son de un emulador debug y no equivalen al rendimiento de un teléfono. El recolector no mide el recorrido de la interfaz ni sustituye una valoración visual. `tools/review_visual_bank.py` compone las comparaciones; no decide puntuaciones.

## Selección previa

Selección fijada antes de procesar: 20/10/2026 (Guayaquil). Diez personas, diez mascotas y diez objetos/dibujos. No hay recortes repetidos ni selecciones posteriores por el resultado del motor.

Versiones: código de producción `5d2f3c8f4679d637a6a17642e41a49e43540ab36` (0.12.0). Auto/Sujeto/Dibujo según el manifiesto, Estructura, Medio, sensibilidad 50, 400 componentes. Las imágenes se reducen hasta 1024 px, sin recorte, respetando proporción y orientación; transparencia sobre blanco. La salida se genera con el `ExportRenderer` real a dibujo completo.

La selección es de conveniencia y no es una muestra estadística: los retratos proceden de NASA, hay perros y gatos y cinco fotografías de objetos más cinco ilustraciones. Incluye sujeto pequeño, grupo, perfiles, fondo complejo, sombra, pelo, hocico cubierto y tramas finas. El resultado no acredita otras poblaciones, fabricantes ni toda la experiencia de edición.

El manifiesto conserva autor, licencia, URL, hash de la descarga y SHA256 de cada entrada normalizada. La preparación aborta si cambian los bytes. Los originales normalizados se conservan en el artefacto de la ejecución. Las licencias son las indicadas para cada imagen, no la licencia genérica de los metadatos de Commons.

| ID | Perfil | Imagen / fuente | Autor | Licencia |
| --- | --- | --- | --- | --- |
| P01 | AUTO | [Charles Bolden astronaut photo.jpg](https://commons.wikimedia.org/wiki/File:Charles_Bolden_astronaut_photo.jpg) | NASA | Public domain |
| P02 | AUTO | [Official portrait of astronaut Linda M. Godwin.jpg](https://commons.wikimedia.org/wiki/File:Official_portrait_of_astronaut_Linda_M._Godwin.jpg) | NASA | Public domain |
| P03 | AUTO | [Susan J. Helms - Official portrait of astronaut candidate.jpg](https://commons.wikimedia.org/wiki/File:Susan_J._Helms_-_Official_portrait_of_astronaut_candidate.jpg) | NASA | Public domain |
| P04 | AUTO | [Janice Voss.jpg](https://commons.wikimedia.org/wiki/File:Janice_Voss.jpg) | NASA | Public domain |
| P05 | AUTO | [Marsha Ivins.jpg](https://commons.wikimedia.org/wiki/File:Marsha_Ivins.jpg) | NASA | Public domain |
| P06 | AUTO | [Samantha Cristoforetti official portrait in an EMU spacesuit.jpg](https://commons.wikimedia.org/wiki/File:Samantha_Cristoforetti_official_portrait_in_an_EMU_spacesuit.jpg) | Robert Markowitz | Public domain |
| P07 | AUTO | [Jim Lovell official 1964 portrait.jpg](https://commons.wikimedia.org/wiki/File:Jim_Lovell_official_1964_portrait.jpg) | NASA | Public domain |
| P08 | AUTO | [The official portrait of the Expedition 73 crew (iss073-s-002).jpg](https://commons.wikimedia.org/wiki/File:The_official_portrait_of_the_Expedition_73_crew_(iss073-s-002).jpg) | NASA Johnson Space Center / Josh Valcarcel – NASA – John | Public domain |
| P09 | AUTO | [Mark and Scott Kelly at the Johnson Space Center, Houston Texas - profile.jpg](https://commons.wikimedia.org/wiki/File:Mark_and_Scott_Kelly_at_the_Johnson_Space_Center,_Houston_Texas_-_profile.jpg) | Robert Markowitz | Public domain |
| P10 | AUTO | [Tracy Caldwell Dyson in Cupola ISS.jpg](https://commons.wikimedia.org/wiki/File:Tracy_Caldwell_Dyson_in_Cupola_ISS.jpg) | NASA/Tracy Caldwell Dyson | Public domain |
| M01 | SUBJECT | [Boxer (dog) , Iran 08.jpg](https://commons.wikimedia.org/wiki/File:Boxer_(dog)_,_Iran_08.jpg) | Mostafameraji | CC0 |
| M02 | SUBJECT | [Dog at Nørre Vorupør Strand.jpg](https://commons.wikimedia.org/wiki/File:Dog_at_N%C3%B8rre_Vorup%C3%B8r_Strand.jpg) | W.carter | CC0 |
| M03 | SUBJECT | [Dog in a muzzle.jpg](https://commons.wikimedia.org/wiki/File:Dog_in_a_muzzle.jpg) | Sobaki2016 | CC0 |
| M04 | SUBJECT | [Mixed-breed dogs in Serles.jpg](https://commons.wikimedia.org/wiki/File:Mixed-breed_dogs_in_Serles.jpg) | Wilfredor | CC0 |
| M05 | SUBJECT | [Panting-dog-No.3.jpg](https://commons.wikimedia.org/wiki/File:Panting-dog-No.3.jpg) | Raffael Herrmann | CC0 |
| M06 | SUBJECT | [Closeup photo of a cat.jpg](https://commons.wikimedia.org/wiki/File:Closeup_photo_of_a_cat.jpg) | RobotBlanket | CC0 |
| M07 | SUBJECT | [Cat - Moomin.jpg](https://commons.wikimedia.org/wiki/File:Cat_-_Moomin.jpg) | Tetizeraz | CC0 |
| M08 | SUBJECT | [Cat in towel.jpg](https://commons.wikimedia.org/wiki/File:Cat_in_towel.jpg) | OboeBlanket | CC0 |
| M09 | SUBJECT | [Cat-on-couch.jpg](https://commons.wikimedia.org/wiki/File:Cat-on-couch.jpg) | Christoffer G. Thomsen | CC0 |
| M10 | SUBJECT | [Cat in backyard.jpg](https://commons.wikimedia.org/wiki/File:Cat_in_backyard.jpg) | John Harvey | Public domain |
| O01 | SUBJECT | [Coffee cup seen from above.jpg](https://commons.wikimedia.org/wiki/File:Coffee_cup_seen_from_above.jpg) | www.Pixel.la Free Stock Photos | CC0 |
| O02 | SUBJECT | [Leonardo's bicycle.jpg](https://commons.wikimedia.org/wiki/File:Leonardo%27s_bicycle.jpg) | User:Brandmeister (Togrul Safarov) | CC0 |
| O03 | SUBJECT | [Large photographic lens with a camera.jpg](https://commons.wikimedia.org/wiki/File:Large_photographic_lens_with_a_camera.jpg) | Tomwsulcer | CC0 |
| O04 | SUBJECT | [Arm Chair MET cds164235.jpg](https://commons.wikimedia.org/wiki/File:Arm_Chair_MET_cds164235.jpg) | Ver ficha de la fuente | CC0 |
| O05 | SUBJECT | [Consumer's Rest chair, V&A Museum 01.jpg](https://commons.wikimedia.org/wiki/File:Consumer%27s_Rest_chair,_V%26A_Museum_01.jpg) | 14GTR | CC0 |
| O06 | DRAWING | [Honey Bee Line Drawing.jpg](https://commons.wikimedia.org/wiki/File:Honey_Bee_Line_Drawing.jpg) | Nate Walton | CC0 |
| O07 | DRAWING | [A line drawing of a lion with wings (public domain).jpg](https://commons.wikimedia.org/wiki/File:A_line_drawing_of_a_lion_with_wings_(public_domain).jpg) | Jerimee | CC0 |
| O08 | DRAWING | [Bouquet (flowers).jpg](https://commons.wikimedia.org/wiki/File:Bouquet_(flowers).jpg) | David Ring | CC0 |
| O09 | DRAWING | [Bicycle (453586) - The Noun Project.svg](https://commons.wikimedia.org/wiki/File:Bicycle_(453586)_-_The_Noun_Project.svg) | Dwanlaposh | CC0 |
| O10 | DRAWING | [Camera by donchico.svg](https://commons.wikimedia.org/wiki/File:Camera_by_donchico.svg) | donchico | CC0 |
