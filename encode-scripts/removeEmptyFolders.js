const fs = require('fs').promises;
const path = require('path');

/**
 * Xóa thư mục rỗng hoặc thư mục chỉ chứa thư mục rỗng (đệ quy)
 * @param {string} dirPath - Đường dẫn đến thư mục cần kiểm tra
 * @returns {Promise<boolean>} - Trả về true nếu thư mục bị xóa, false nếu không
 */
async function removeEmptyFolders(dirPath) {
  try {
    
    const stat = await fs.stat(dirPath);
    if (!stat.isDirectory()) {
      return false; 
    }

    
    const items = await fs.readdir(dirPath);

    
    let isEmpty = true;
    for (const item of items) {
      const fullPath = path.join(dirPath, item);
      const itemStat = await fs.stat(fullPath);

      if (itemStat.isDirectory()) {
        
        const subDirEmpty = await removeEmptyFolders(fullPath);
        if (!subDirEmpty) {
          isEmpty = false; 
        }
      } else {
        isEmpty = false; 
      }
    }

    
    if (isEmpty) {
      await fs.rmdir(dirPath);
      console.log(`Đã xóa thư mục rỗng: ${dirPath}`);
      return true;
    }

    return false;
  } catch (error) {
    console.error(`Lỗi khi xử lý thư mục ${dirPath}:`, error.message);
    return false;
  }
}


async function main() {
  const targetDir = process.argv[2] || process.cwd(); 
  console.log(`Kiểm tra và xóa thư mục rỗng trong: ${targetDir}`);
  await removeEmptyFolders(targetDir);
  console.log('Hoàn tất!');
}


main().catch(error => {
  console.error('Lỗi:', error.message);
  process.exit(1);
});