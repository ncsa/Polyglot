#!/bin/bash
#ebook-convert (v1.31)
#ebook
#lrf, rar, zip, rtf, lit, txt, txtz, text, htm, xhtm, html, htmlz, xhtml, pdf, pdb, updb, pdr, prc, mobi, azw, docx, epub, fb2, djv, djvu, lrx, cbr, cbz, cbc, oebzip, rb, imp, odt, chm, tpz, azw1, pml, pmlz, mbp, tan, snb, xps, oxps, azw4, book, zbf, pobi, docm, md, textile, markdown, ibook, iba, azw3, ps
#epub, mobi, azw3, fb2, htmlz, lit, lrf, pdb, pdf, pmlz, rb, rtf, snb, tcr, txt, txtz, zip

xvfb-run ebook-convert "$1" "$2" > /dev/null

