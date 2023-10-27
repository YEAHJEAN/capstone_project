const express = require('express');
const bodyParser = require('body-parser');
const mysql = require('mysql2');
const app = express();
const port = 3001;

app.use(bodyParser.json());

// MySQL 연결 설정
const db = mysql.createConnection({
  host: 'localhost',
  user: 'root',
  password: '12345678',
  database: 'users'
});

// MySQL 연결
db.connect((err) => {
  if (err) {
    console.error('MySQL 연결 실패: ' + err.message);
  } else {
    console.log('MySQL에 연결되었습니다.');
  }
});

// 로그인 엔드포인트
app.post('/login', (req, res) => {
  const { id, password } = req.body;

  // 간단한 유효성 검사
  if (!id || !password) {
    return res.status(400).json({ success: false, message: '아이디 또는 비밀번호가 누락되었습니다.' });
  }

  // 사용자 아이디와 비밀번호 확인
  db.query('SELECT * FROM users WHERE id = ? AND password = ?', [id, password], (error, results) => {
    if (error) {
      console.error(error);
      return res.status(500).json({ success: false, message: '서버 오류' });
    }

    if (results.length === 0) {
      return res.status(401).json({ success: false, message: '로그인 실패: 아이디 또는 비밀번호가 일치하지 않습니다.' });
    }

    // 로그인 성공
    res.status(200).json({ success: true, message: '로그인 성공!' });
  });
});

app.listen(port, () => {
  console.log(`서버가 http://localhost:${port} 포트에서 실행 중입니다.`);
});
