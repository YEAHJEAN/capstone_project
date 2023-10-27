const express = require('express');
const bodyParser = require('body-parser');
const mysql = require('mysql2');
const app = express();
const port = 3001;

app.use(bodyParser.json());

// MySQL 연결 설정
const db = mysql.createConnection({
  host: 'localhost', // MySQL 호스트
  user: 'root', // MySQL 사용자
  password: '12345678', // MySQL 비밀번호
  database: 'users' // 사용할 데이터베이스 이름
  
});

// MySQL 연결
db.connect((err) => {
  if (err) {
    console.error('MySQL 연결 실패: ' + err.message);
  } else {
    console.log('MySQL에 연결되었습니다.');
  }
});

// 사용자 등록 엔드포인트
app.post('/register', (req, res) => {
  const userData = req.body;

  // 간단한 유효성 검사
  if (!userData.id || !userData.password || !userData.email) {
    return res.status(400).json({ success: false, message: '누락된 데이터가 있습니다.' });
  }

  // 이미 존재하는 아이디인지 확인
  db.query('SELECT * FROM users WHERE id = ?', [userData.id], (error, results) => {
    if (error) {
      console.error(error);
      return res.status(500).json({ success: false, message: '서버 오류' });
    }

    if (results.length > 0) {
      // 이미 등록된 아이디가 존재하는 경우
      return res.status(409).json({ success: false, message: '이미 등록된 아이디입니다.' });
    }

    // 새로운 사용자를 데이터베이스에 추가
    db.query('INSERT INTO users (id, password, email) VALUES (?, ?, ?)', [userData.id, userData.password, userData.email], (error, results) => {
      if (error) {
        console.error(error);
        return res.status(500).json({ success: false, message: '서버 오류' });
      }

      res.status(201).json({ success: true, message: '회원가입이 성공했습니다.' });
    });
  });
});

app.listen(port, () => {
  console.log(`서버가 http://localhost:${port} 포트에서 실행 중입니다.`);
});
