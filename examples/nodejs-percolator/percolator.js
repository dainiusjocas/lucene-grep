const {spawn} = require('child_process');

const grep = spawn('lmgrep', ['test'] );

grep.stdout.on('data', (data) => {
    console.log(`>>>>: ${data}`);
});

grep.stderr.on('data', (data) => {
    console.error(`stderr: ${data}`);
});

grep.on('close', (code) => {
    console.log(`child process exited with code ${code}`);
});

grep.stdin.setEncoding('utf-8');

grep.stdin.write("foo test bar\n");

grep.stdin.end();

console.log("Done");
