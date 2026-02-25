const { defineConfig } = require("cypress");
const xlsx = require("xlsx");
const path = require("path");

module.exports = defineConfig({
  e2e: {
    setupNodeEvents(on, config) {
      on('task', {
        readExcel(filePath) {
          const absolutePath = path.resolve(__dirname, filePath);
          const workbook = xlsx.readFile(absolutePath);

          // Đọc sheet Config
          const configSheet = workbook.Sheets["Config"];
          const configData = xlsx.utils.sheet_to_json(configSheet);
          // Chuyển array [{key: 'baseUrl', value: '...'}, ...] thành object {baseUrl: '...'}
          const configObj = {};
          configData.forEach(item => configObj[item.key] = item.value);

          // Đọc sheet TestCases
          const tcSheet = workbook.Sheets["TestCases"];
          const tcData = xlsx.utils.sheet_to_json(tcSheet);
          // Chuyển array thành object có key là mã TD (TD01, TD02...)
          const testCasesObj = {};
          tcData.forEach(item => {
            testCasesObj[item.tcID] = item;
          });

          return { config: configObj, testCases: testCasesObj };
        }
      });
    },
  },
});