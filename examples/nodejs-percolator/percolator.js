const {spawn} = require('child_process');

class Percolator {

    constructor(query) {
        this.a = null;
        this.grep = spawn('lmgrep', [query] );

        this.grep.stdout.on('data', (data) => {
            console.log(`>>>><<<: ${data}`);
            this.a = data;
        });

        this.grep.stderr.on('data', (data) => {
            console.error(`stderr: ${data}`);
        });

        this.grep.on('close', (code) => {
            console.log(`child process exited with code ${code}`);
        });

        this.grep.stdin.setEncoding('utf-8');
    }

    percolate(text) {
        this.grep.stdin.write(text + "\n");
        return this.a;
    }

    close() {
        this.grep.stdin.end();
    }
}

const percolator = new Percolator('test');

resp = percolator.percolate("foo test bar")

console.log(">>>>" + resp);

percolator.close();

console.log("Done");
