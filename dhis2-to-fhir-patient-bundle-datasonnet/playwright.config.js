const {
    defineConfig,
    devices
} = require('@playwright/test');

module.exports = defineConfig({
    testDir: './tests',
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 2 : 0,
    workers: process.env.CI ? 1 : undefined,
    reporter: 'html',
    use: {
        baseURL: 'http://localhost:8080/',
        extraHTTPHeaders: {
            'Content-Type': 'application/json',
            'Authorization': 'Basic YWRtaW46ZGlzdHJpY3Q=',
        },
        trace: 'on-first-retry',
    },

    projects: [{
        name: 'chromium',
        use: {
            ...devices['Desktop Chrome']
        },
    }, ],

    webServer: {
        command: 'docker compose -f esignet-docker-compose.yml -f docker-compose.yml -f tests/docker-compose.test.yml up --build --renew-anon-volumes --force-recreate --remove-orphans',
        url: 'http://localhost:8080',
        stdout: 'pipe',
        stderr: 'pipe',
        reuseExistingServer: true,
        timeout: 300000
    },
});