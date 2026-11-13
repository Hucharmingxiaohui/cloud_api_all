// Generate a self-contained DOCX from the maintained Markdown and captured PNGs.
const fs = require('fs')
const path = require('path')
const JSZip = require('jszip')
const root = path.resolve('md/工地巡检平台操作手册')
const title = '工地巡检平台操作手册'
const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
const w = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
const r = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships'
const relns = 'http://schemas.openxmlformats.org/package/2006/relationships'
const para = (text, style = 'Normal', extra = '') => `<w:p><w:pPr><w:pStyle w:val="${style}"/>${extra}</w:pPr><w:r><w:t xml:space="preserve">${esc(text)}</w:t></w:r></w:p>`
async function main () {
  const lines = fs.readFileSync(path.join(root, title + '.md'), 'utf8').split(/\r?\n/)
  const zip = new JSZip(); let body = ''; let cover = true; let imageId = 0
  const rels = [
    ['styles', 'styles', 'styles.xml'], ['header', 'header', 'header1.xml'], ['footer', 'footer', 'footer1.xml']
  ]
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim(); if (!line) continue
    if (line.startsWith('# ')) { body += para(line.slice(2), 'Title'); continue }
    if (line.startsWith('## ')) { cover = false; body += para(line.slice(3), 'Heading1'); continue }
    if (line.startsWith('### ')) { body += para(line.slice(4), 'Heading2'); continue }
    const image = line.match(/^!\[(.*)\]\((.*)\)$/)
    if (image) {
      const bytes = fs.readFileSync(path.join(root, image[2])); const id = ++imageId
      const width = bytes.readUInt32BE(16); const height = bytes.readUInt32BE(20)
      const cx = 5943600; const cy = Math.round(cx * height / width)
      zip.file(`word/media/image${id}.png`, bytes); rels.push(['img' + id, 'image', `media/image${id}.png`])
      body += `<w:p><w:pPr><w:jc w:val="center"/><w:keepNext/><w:spacing w:before="160" w:after="80"/></w:pPr><w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="${cx}" cy="${cy}"/><wp:docPr id="${id}" name="${esc(image[1])}" descr="${esc(image[1])}"/><wp:cNvGraphicFramePr><a:graphicFrameLocks noChangeAspect="1"/></wp:cNvGraphicFramePr><a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic><pic:nvPicPr><pic:cNvPr id="${id}" name="image${id}.png"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed="img${id}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="${cx}" cy="${cy}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>`
      body += para(image[1], 'Caption'); continue
    }
    if (line.startsWith('|')) {
      const rows = []
      while (i < lines.length && lines[i].trim().startsWith('|')) {
        const cells = lines[i].trim().slice(1, -1).split('|').map(x => x.trim())
        if (!cells.every(c => /^[-: ]+$/.test(c))) rows.push(cells)
        i++
      }
      i--
      const count = rows[0].length; const width = Math.floor(9360 / count)
      body += `<w:tbl><w:tblPr><w:tblW w:w="9360" w:type="dxa"/><w:tblLayout w:type="fixed"/><w:tblBorders>${['top', 'left', 'bottom', 'right', 'insideH', 'insideV'].map(s => `<w:${s} w:val="single" w:sz="4" w:color="D5E1EA"/>`).join('')}</w:tblBorders><w:tblCellMar><w:top w:w="100" w:type="dxa"/><w:left w:w="120" w:type="dxa"/><w:bottom w:w="100" w:type="dxa"/><w:right w:w="120" w:type="dxa"/></w:tblCellMar></w:tblPr><w:tblGrid>${rows[0].map(() => `<w:gridCol w:w="${width}"/>`).join('')}</w:tblGrid>`
      rows.forEach((row, n) => {
        body += `<w:tr><w:trPr><w:cantSplit/>${n === 0 ? '<w:tblHeader/>' : ''}</w:trPr>`
        row.forEach(cell => { body += `<w:tc><w:tcPr><w:tcW w:w="${width}" w:type="dxa"/><w:shd w:fill="${n === 0 ? '174A70' : n % 2 ? 'F1F6FA' : 'FFFFFF'}"/></w:tcPr>${para(cell, n === 0 ? 'TableHeader' : 'TableText')}</w:tc>` })
        body += '</w:tr>'
      }); body += '</w:tbl>' + para(''); continue
    }
    body += para(line, cover ? 'CoverText' : line.startsWith('读图指引：') ? 'Guide' : 'Normal')
  }
  body += '<w:sectPr><w:headerReference w:type="default" r:id="header"/><w:footerReference w:type="default" r:id="footer"/><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1273" w:bottom="1134" w:left="1273" w:header="567" w:footer="567"/></w:sectPr>'
  const xml = s => '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' + s
  zip.file('word/document.xml', xml(`<w:document xmlns:w="${w}" xmlns:r="${r}" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><w:body>${body}</w:body></w:document>`))
  const style = (id, name, p, run) => `<w:style w:type="paragraph" w:styleId="${id}"><w:name w:val="${name}"/><w:basedOn w:val="Normal"/><w:pPr>${p}</w:pPr><w:rPr>${run}</w:rPr></w:style>`
  zip.file('word/styles.xml', xml(`<w:styles xmlns:w="${w}"><w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="宋体"/><w:sz w:val="22"/><w:color w:val="263445"/></w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after="140" w:line="330" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults><w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style>${style('Title', 'Title', '<w:jc w:val="center"/><w:spacing w:before="2400" w:after="1000"/>', '<w:b/><w:sz w:val="56"/><w:color w:val="174A70"/>')}${style('CoverText', 'Cover Text', '<w:jc w:val="center"/><w:spacing w:after="400"/>', '<w:sz w:val="26"/>')}${style('Heading1', 'heading 1', '<w:pageBreakBefore/><w:keepNext/><w:outlineLvl w:val="0"/><w:spacing w:after="320"/>', '<w:b/><w:sz w:val="36"/><w:color w:val="174A70"/>')}${style('Heading2', 'heading 2', '<w:keepNext/><w:outlineLvl w:val="1"/><w:spacing w:before="260" w:after="180"/>', '<w:b/><w:sz w:val="27"/><w:color w:val="174A70"/>')}${style('Caption', 'Caption', '<w:jc w:val="center"/><w:spacing w:after="200"/>', '<w:sz w:val="19"/><w:color w:val="64748B"/>')}${style('Guide', 'Figure Guide', '<w:spacing w:after="180"/><w:shd w:fill="F1F6FA"/>', '<w:sz w:val="20"/><w:color w:val="174A70"/>')}${style('TableHeader', 'Table Header', '<w:spacing w:after="20" w:line="280"/>', '<w:b/><w:sz w:val="20"/><w:color w:val="FFFFFF"/>')}${style('TableText', 'Table Text', '<w:spacing w:after="20" w:line="280"/>', '<w:sz w:val="20"/>')}</w:styles>`))
  zip.file('word/header1.xml', xml(`<w:hdr xmlns:w="${w}">${para('东方电子股份有限公司  |  工地巡检平台操作手册', 'Caption', '<w:jc w:val="right"/>')}</w:hdr>`))
  zip.file('word/footer1.xml', xml(`<w:ftr xmlns:w="${w}"><w:p><w:pPr><w:jc w:val="center"/></w:pPr><w:r><w:rPr><w:sz w:val="18"/></w:rPr><w:t>V1.0 · 2026-11-05   |   第 </w:t></w:r><w:fldSimple w:instr="PAGE"/><w:r><w:t> 页</w:t></w:r></w:p></w:ftr>`))
  zip.file('word/_rels/document.xml.rels', xml(`<Relationships xmlns="${relns}">${rels.map(([id, type, target]) => `<Relationship Id="${id}" Type="${r}/${type}" Target="${target}"/>`).join('')}</Relationships>`))
  zip.file('_rels/.rels', xml(`<Relationships xmlns="${relns}"><Relationship Id="doc" Type="${r}/officeDocument" Target="word/document.xml"/><Relationship Id="core" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/></Relationships>`))
  zip.file('docProps/core.xml', xml(`<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>${title}</dc:title><dc:creator>东方电子股份有限公司</dc:creator><dc:description>固定与移动部署图文操作手册，截图使用演示数据。</dc:description></cp:coreProperties>`))
  zip.file('[Content_Types].xml', xml(`<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Default Extension="png" ContentType="image/png"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/><Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/><Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/></Types>`))
  const output = path.join(root, title + '.docx'); fs.writeFileSync(output, await zip.generateAsync({ type: 'nodebuffer', compression: 'DEFLATE' }))
  console.log(JSON.stringify({ output, screenshots: imageId, characters: lines.join('').length }))
}
main().catch(e => { console.error(e); process.exitCode = 1 })
