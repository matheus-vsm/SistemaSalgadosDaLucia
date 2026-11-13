const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const base = process.env.SMOKE_BASE_URL || 'http://127.0.0.1:15500';
const prefix = '/api/salgados-da-lucia-kojima';

test('frontend, proxy e validacao HTTP com backend real', async () => {
    let ready = false;
    for (let attempt = 0; attempt < 90; attempt++) {
        try {
            const response = await fetch(base + prefix + '/autenticacao/login', {
                method: 'POST', headers: {'Content-Type': 'application/json'}, body: '{}',
                signal: AbortSignal.timeout(3000)
            });
            if (response.status === 400) { ready = true; break; }
        } catch {}
        await new Promise(resolve => setTimeout(resolve, 1000));
    }
    assert.ok(ready, 'backend deve ficar pronto pelo proxy em ate 90 tentativas');
    const frontend = path.resolve(__dirname, '../../frontend');
    async function check(directory, relative = '') {
        for (const file of fs.readdirSync(directory, {withFileTypes: true})) {
            if (file.name === 'tests' || file.name === 'Dockerfile') continue;
            const location = path.join(directory, file.name);
            const url = `${relative}/${file.name}`;
            if (file.isDirectory()) await check(location, url);
            else {
                const response = await fetch(base + url);
                assert.equal(response.status, 200, url);
                assert.equal(response.headers.get('cache-control'), 'no-cache', url);
                assert.deepEqual(Buffer.from(await response.arrayBuffer()), fs.readFileSync(location), url);
            }
        }
    }
    await check(frontend);
    for (const url of ['/missing.html', '/.git/config', '/tests/api.test.cjs', '/backend/pom.xml']) {
        assert.equal((await fetch(base + url)).status, 404, url);
    }
    const invalid = await fetch(base + prefix + '/autenticacao/login', {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: '{}'
    });
    assert.equal(invalid.status, 400, 'validacao deve vir do Spring, nao do Nginx');
    assert.match(invalid.headers.get('content-type'), /json/);
    const protectedResponse = await fetch(base + prefix + '/clientes?ativo=true&page=0&size=1');
    assert.ok([401, 403].includes(protectedResponse.status), 'consulta sem token deve ser negada');
});
