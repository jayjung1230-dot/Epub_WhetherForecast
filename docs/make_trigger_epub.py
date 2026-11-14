import os
import zipfile
from PIL import Image

BASE_DIR = r"C:\Users\reevo\Desktop\앱개발프로젝트\Ridibooks Paper Pro"
COVER_PNG = os.path.join(BASE_DIR, "docs", "library_book_cover_sample.png")
OUT_EPUB = os.path.join(BASE_DIR, "docs", "weather_dashboard_trigger.epub")

def build_epub():
    cover_jpg_bytes = None
    img = Image.open(COVER_PNG).convert("RGB")
    from io import BytesIO
    buf = BytesIO()
    img.save(buf, format="JPEG", quality=92)
    cover_jpg_bytes = buf.getvalue()

    container_xml = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>"""

    content_opf = """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookId" version="2.0">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
    <dc:title>날씨 대시보드</dc:title>
    <dc:creator opf:role="aut">PaperPro Weather</dc:creator>
    <dc:language>ko</dc:language>
    <dc:identifier id="BookId">urn:uuid:ridipaper-weather-dashboard-001</dc:identifier>
    <meta name="cover" content="cover-image"/>
  </metadata>
  <manifest>
    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
    <item id="cover-image" href="cover.jpg" media-type="image/jpeg"/>
    <item id="page1" href="index.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine toc="ncx">
    <itemref idref="page1"/>
  </spine>
  <guide>
    <reference type="cover" title="Cover" href="index.xhtml"/>
  </guide>
</package>"""

    toc_ncx = """<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
  <head>
    <meta name="dtb:uid" content="urn:uuid:ridipaper-weather-dashboard-001"/>
  </head>
  <docTitle><text>날씨 대시보드</text></docTitle>
  <navMap>
    <navPoint id="nav1" playOrder="1">
      <navLabel><text>날씨 대시보드</text></navLabel>
      <content src="index.xhtml"/>
    </navPoint>
  </navMap>
</ncx>"""

    index_xhtml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>날씨 대시보드</title></head>
<body style="text-align:center; font-family:sans-serif; padding-top:40%;">
  <h2>날씨 대시보드 실행 중...</h2>
  <p>자동으로 전환되지 않으면 앱 서비스가 실행 중인지 확인해 주세요.</p>
</body>
</html>"""

    with zipfile.ZipFile(OUT_EPUB, "w") as zf:
        zf.writestr("mimetype", "application/epub+zip", compress_type=zipfile.ZIP_STORED)
        zf.writestr("META-INF/container.xml", container_xml, compress_type=zipfile.ZIP_DEFLATED)
        zf.writestr("OEBPS/content.opf", content_opf, compress_type=zipfile.ZIP_DEFLATED)
        zf.writestr("OEBPS/toc.ncx", toc_ncx, compress_type=zipfile.ZIP_DEFLATED)
        zf.writestr("OEBPS/index.xhtml", index_xhtml, compress_type=zipfile.ZIP_DEFLATED)
        zf.writestr("OEBPS/cover.jpg", cover_jpg_bytes, compress_type=zipfile.ZIP_DEFLATED)

    print("Created:", OUT_EPUB)

if __name__ == "__main__":
    build_epub()
