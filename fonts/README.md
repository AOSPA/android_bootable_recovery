Inconsolata
-----------
The images 12x22.png and 18x32.png were generated using the font Inconsolata,
which is released under the OFL license and was obtained from:
https://code.google.com/p/googlefontdirectory/source/browse/ofl/inconsolata/

RobotoMono
----------
The image 22x42.png is generated using the font RobotoMono, which is under
Apache License, Version 2.0. The ttf files can be obtained from:
https://fonts.google.com/specimen/Roboto+Mono

FontGenerator
-------------
There is a host java program under `tools/recovery_image_generator/` which can
generate these font files. Steps to run:

    mma -j32
    java -jar $output_jar_path $regular_font $bold_font $output_path


