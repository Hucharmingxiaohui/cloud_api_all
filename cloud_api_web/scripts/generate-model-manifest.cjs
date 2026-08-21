const fs = require('fs')
const path = require('path')

const modelRoot = path.resolve(__dirname, '../public/model')
const outputFile = path.join(modelRoot, 'models.json')

function readJson (filePath) {
  try {
    return JSON.parse(fs.readFileSync(filePath, 'utf8'))
  } catch (error) {
    return null
  }
}

function isTilesetEntry (filePath) {
  const data = readJson(filePath)
  return Boolean(data && data.asset && data.root)
}

function toUrl (filePath) {
  const relativePath = path.relative(modelRoot, filePath).split(path.sep).join('/')
  return `/model/${relativePath}`
}

function findJsonEntries (dirPath, depth = 0) {
  if (depth > 2) return []

  const entries = fs.readdirSync(dirPath, { withFileTypes: true })
  const files = []

  for (const entry of entries) {
    const entryPath = path.join(dirPath, entry.name)
    if (entry.isFile() && entry.name.endsWith('.json') && isTilesetEntry(entryPath)) {
      files.push(entryPath)
    }
    if (entry.isDirectory() && !['Data', 'Tile_+000_+000'].includes(entry.name)) {
      files.push(...findJsonEntries(entryPath, depth + 1))
    }
  }

  return files
}

function findModelEntry (dirPath) {
  const rootTileset = path.join(dirPath, 'tileset.json')
  if (fs.existsSync(rootTileset) && isTilesetEntry(rootTileset)) {
    return rootTileset
  }

  const jsonEntries = findJsonEntries(dirPath)
  if (jsonEntries.length === 0) return null

  const sceneEntry = jsonEntries.find(filePath => path.relative(dirPath, filePath).split(path.sep).includes('Scene'))
  return sceneEntry || jsonEntries[0]
}

function generateManifest () {
  if (!fs.existsSync(modelRoot)) {
    fs.mkdirSync(modelRoot, { recursive: true })
  }

  const models = fs.readdirSync(modelRoot, { withFileTypes: true })
    .filter(entry => entry.isDirectory())
    .map(entry => {
      const entryPath = path.join(modelRoot, entry.name)
      const modelEntry = findModelEntry(entryPath)
      if (!modelEntry) return null

      return {
        label: entry.name,
        value: entry.name,
        url: toUrl(modelEntry)
      }
    })
    .filter(Boolean)
    .sort((a, b) => a.label.localeCompare(b.label))

  fs.writeFileSync(outputFile, `${JSON.stringify(models, null, 2)}\n`, 'utf8')
  console.log(`Generated ${models.length} model entries at ${outputFile}`)
}

generateManifest()
