const express = require('express');
const bodyParser = require('body-parser');
const mysql = require('mysql2');
const app = express();
const port = 3001;

app.use(bodyParser.urlencoded({ extended: true }));
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


// 사용자 로그인 엔드포인트
app.post('/login', (req, res) => {
  const userData = req.body;

  // 간단한 유효성 검사
  if (!userData.id || !userData.password) {
    return res.status(400).json({ success: false, message: '누락된 데이터가 있습니다.' });
  }

  // 사용자 아이디와 비밀번호를 데이터베이스에서 확인
  db.query('SELECT * FROM users WHERE id = ? AND password = ?', [userData.id, userData.password], (error, results) => {
    if (error) {
      console.error(error);
      return res.status(500).json({ success: false, message: '서버 오류' });
    }

    if (results.length === 0) {
      // 일치하는 사용자가 없는 경우
      return res.status(401).json({ success: false, message: '아이디 또는 비밀번호가 잘못되었습니다.' });
    }

    // 로그인 성공
    res.status(200).json({ success: true, message: '로그인 성공' });
  });
});


// 사용자 정보 불러오기 엔드포인트
app.get('/user_info', (req, res) => {
  const userInfo = req.headers.authorization.split(':'); // Authorization 헤더에서 id와 password 분리
  const id = userInfo[0];
  const password = userInfo[1];

  // 아이디와 비밀번호로 사용자 정보를 데이터베이스에서 확인
  db.query('SELECT * FROM users WHERE id = ? AND password = ?', [id, password], (error, results) => {
    if (error) {
      console.error(error);
      return res.status(500).json({ success: false, message: '서버 오류' });
    }

    if (results.length === 0) {
      // 일치하는 사용자가 없는 경우
      return res.status(401).json({ success: false, message: '아이디 또는 비밀번호가 잘못되었습니다.' });
    }

    // 사용자 정보 전달
    const user = results[0];
    res.status(200).json({ success: true, id: user.id, password: user.password, email: user.email });
  });
});


// 사용자 이메일 변경 엔드포인트
app.post('/change_email', (req, res) => {
  const updateData = req.body;

  // 유효성 검사
  if (!updateData.id || !updateData.email) {
    return res.status(400).json({ success: false, message: '누락된 데이터가 있습니다.' });
  }

  // 사용자 아이디로 구성된 사용자 정보를 데이터베이스에서 확인
  db.query('SELECT * FROM users WHERE id = ?', [updateData.id], (error, results) => {
    if (error) {
      console.error(error);
      return res.status(500).json({ success: false, message: '서버 오류' });
    }

    if (results.length === 0) {
      // 일치하는 사용자가 없는 경우
      return res.status(401).json({ success: false, message: '일치하는 사용자 정보가 없습니다.' });
    }

    // 사용자 정보 업데이트
    db.query('UPDATE users SET email = ? WHERE id = ?', [updateData.email, updateData.id], (error, results) => {
      if (error) {
        console.error(error);
        return res.status(500).json({ success: false, message: '서버 오류' });
      }

      res.status(200).json({ success: true, message: '사용자 정보가 업데이트되었습니다.' });
    });
  });
});


// 사용자 비밀번호 변경 엔드포인트
app.post('/change_password', (req, res) => {
  const updateData = req.body;

  // 유효성 검사
  if (!updateData.id || !updateData.oldPassword || !updateData.newPassword) {
    return res.status(400).json({ success: false, message: '누락된 데이터가 있습니다.' });
  }

  // 사용자 아이디와 기존 비밀번호로 사용자 정보를 데이터베이스에서 확인
  db.query('SELECT * FROM users WHERE id = ? AND password = ?', [updateData.id, updateData.oldPassword], (error, results) => {
    if (error) {
      console.error(error);
      return res.status(500).json({ success: false, message: '서버 오류' });
    }

    if (results.length === 0) {
      // 일치하는 사용자가 없는 경우
      return res.status(401).json({ success: false, message: '일치하는 사용자 정보가 없습니다.' });
    }

    // 사용자 비밀번호 변경
    db.query('UPDATE users SET password = ? WHERE id = ?', [updateData.newPassword, updateData.id], (error, results) => {
      if (error) {
        console.error(error);
        return res.status(500).json({ success: false, message: '서버 오류' });
      }

      res.status(200).json({ success: true, message: '비밀번호가 변경되었습니다.' });
    });
  });
});



// 게시글 작성 엔드포인트
app.post('/posts/create', (req, res) => {
  const postData = req.body;

  // 간단한 유효성 검사
  if (!postData.id || !postData.title || !postData.content) {
    return res.status(400).json({ success: false, message: 'ID, 제목 또는 내용이 누락되었습니다.' });
  }

  // 현재 시간을 MySQL DATETIME 포맷으로 변환
  const createdAt = new Date().toISOString().slice(0, 19).replace('T', ' ');

  // 게시글을 데이터베이스에 추가
  db.query('INSERT INTO posts (id, title, content, created_at) VALUES (?, ?, ?, ?)', [postData.id, postData.title, postData.content, createdAt], (error, results) => {
    if (error) {
      console.error(error);
      return res.status(500).json({ success: false, message: '게시글 작성 중 오류가 발생했습니다.' });
    }

    res.status(201).json({ success: true, message: '게시글이 작성되었습니다.' });
  });
});


// 모든 게시글 가져오기 엔드포인트
app.get('/posts', (req, res) => {

  // 모든 게시글을 데이터베이스에서 가져오기
  db.query('SELECT * FROM posts', (error, results) => {
    if (error) {
      console.error(error);
      return res.status(500).json({ success: false, message: '서버 오류' });
    }

      res.status(200).json(results);
  });
});


// 사용자 탈퇴 엔드포인트
app.post('/delete', (req, res) => {
  const deleteData = req.body;

  // 유효성 검사
  if (!deleteData.id) {
    return res.status(400).json({ success: false, message: '누락된 데이터가 있습니다.' });
  }

  // 사용자 아이디로 사용자 정보를 데이터베이스에서 확인
  db.query('SELECT * FROM users WHERE id = ?', [deleteData.id], (error, results) => {
    if (error) {
      console.error(error);
      return res.status(500).json({ success: false, message: '서버 오류' });
    }

    if (results.length === 0) {
      // 일치하는 사용자가 없는 경우
      return res.status(401).json({ success: false, message: '일치하는 사용자 정보가 없습니다.' });
    }

    // 사용자 정보 삭제
    db.query('DELETE FROM users WHERE id = ?', [deleteData.id], (error, results) => {
      if (error) {
        console.error(error);
        return res.status(500).json({ success: false, message: '서버 오류' });
      }

      res.status(200).json({ success: true, message: '사용자 정보가 삭제되었습니다.' });
    });
  });
});


// /books 엔드포인트에 대한 GET 요청 처리
app.get('/books', (req, res) => {
  const userId = req.query.userId; // 사용자 ID

  // 사용자 ID에 해당하는 책 목록을 가져오기 위한 SQL 쿼리
  const sql = 'SELECT * FROM texts WHERE user_id = ?'; // 쿼리 수정: userId -> user_id

  // 데이터베이스에서 책 목록을 가져옵니다.
  db.query(sql, [userId], (err, result) => {
    if (err) {
      console.error('Error fetching books from database:', err);
      res.status(500).send('Error fetching books from database');
      return;
    }


    // 책 목록을 JSON 형태로 클라이언트에게 반환합니다.
    res.json(result);
  });
});


app.post('/saveToDatabase', (req, res) => {
  const data = req.body;
      const userid = data.userid;
      const texts = data.texts;
  
      const values = texts.map(({ title, author }) => `('${userid}', '${title}', '${author}')`).join(', ');
    
      // MySQL에 정보 삽입
      const sql = `INSERT INTO texts (user_id, title, author) VALUES ${values}`;
  
      db.query(sql, (err, result) => {
          if (err) {
              console.error('Error saving data to database:', err);
              res.status(500).send('Error saving data to database');
              return;
          }
          console.log('Data saved to database');
          res.status(200).send('Data saved to database');
      });
  });


app.listen(port, () => {
  console.log(`서버가 http://localhost:${port} 포트에서 실행 중입니다.`);
});