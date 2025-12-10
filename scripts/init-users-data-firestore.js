/**
 * Script para crear datos de ejemplo de usuarios y exam_attempts en Firestore
 * 
 * Este script crea múltiples usuarios con diferentes métricas y sus intentos de examen
 * para poder probar el leaderboard en la app.
 * 
 * Uso:
 *   node scripts/init-users-data-firestore.js
 * 
 * Requisitos:
 *   - serviceAccountKey.json en la raíz del proyecto
 *   - firebase-admin instalado: npm install firebase-admin
 */

const admin = require('firebase-admin');
const path = require('path');

// Inicializar Firebase Admin
const serviceAccountPath = path.join(__dirname, '..', 'serviceAccountKey.json');

try {
  const serviceAccount = require(serviceAccountPath);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  console.log('✅ Firebase Admin inicializado correctamente');
} catch (error) {
  console.error('❌ Error al inicializar Firebase Admin:', error.message);
  console.error('   Asegúrate de que serviceAccountKey.json existe en la raíz del proyecto');
  process.exit(1);
}

const db = admin.firestore();

// Datos de ejemplo: usuarios con diferentes niveles de rendimiento
// NOTA: Las métricas se calcularán REALMENTE desde los intentos de examen creados
// La foto de perfil viene de Gmail (Firebase Auth), por eso es null en estos ejemplos
const exampleUsers = [
  {
    uid: 'user_demo_001',
    displayName: 'María González',
    email: 'maria.gonzalez@example.com',
    schoolCode: 'UGEL-001',
    photoUrl: null, // En usuarios reales viene de Firebase Auth (Gmail)
    // Configuración de intentos (las métricas se calcularán desde estos)
    attempts: 15,
    // Rango de aciertos por intento (para simular variación real)
    minCorrect: 8,
    maxCorrect: 10
  },
  {
    uid: 'user_demo_002',
    displayName: 'Carlos Rodríguez',
    email: 'carlos.rodriguez@example.com',
    schoolCode: 'UGEL-001',
    photoUrl: null,
    attempts: 12,
    minCorrect: 7,
    maxCorrect: 9
  },
  {
    uid: 'user_demo_003',
    displayName: 'Ana Martínez',
    email: 'ana.martinez@example.com',
    schoolCode: 'UGEL-001',
    photoUrl: null,
    attempts: 10,
    minCorrect: 6,
    maxCorrect: 8
  },
  {
    uid: 'user_demo_004',
    displayName: 'Luis Fernández',
    email: 'luis.fernandez@example.com',
    schoolCode: 'UGEL-002',
    photoUrl: null,
    attempts: 18,
    minCorrect: 9,
    maxCorrect: 10
  },
  {
    uid: 'user_demo_005',
    displayName: 'Sofía López',
    email: 'sofia.lopez@example.com',
    schoolCode: 'UGEL-002',
    photoUrl: null,
    attempts: 11,
    minCorrect: 7,
    maxCorrect: 9
  },
  {
    uid: 'user_demo_006',
    displayName: 'Diego Sánchez',
    email: 'diego.sanchez@example.com',
    schoolCode: 'UGEL-002',
    photoUrl: null,
    attempts: 8,
    minCorrect: 5,
    maxCorrect: 7
  },
  {
    uid: 'user_demo_007',
    displayName: 'Valentina Torres',
    email: 'valentina.torres@example.com',
    schoolCode: 'UGEL-003',
    photoUrl: null,
    attempts: 13,
    minCorrect: 8,
    maxCorrect: 9
  },
  {
    uid: 'user_demo_008',
    displayName: 'Andrés Ramírez',
    email: 'andres.ramirez@example.com',
    schoolCode: 'UGEL-003',
    photoUrl: null,
    attempts: 7,
    minCorrect: 4,
    maxCorrect: 6
  },
  {
    uid: 'user_demo_009',
    displayName: 'Camila Herrera',
    email: 'camila.herrera@example.com',
    schoolCode: 'UGEL-003',
    photoUrl: null,
    attempts: 10,
    minCorrect: 6,
    maxCorrect: 8
  },
  {
    uid: 'user_demo_010',
    displayName: 'Sebastián Jiménez',
    email: 'sebastian.jimenez@example.com',
    schoolCode: 'UGEL-001',
    photoUrl: null,
    attempts: 6,
    minCorrect: 4,
    maxCorrect: 6
  }
];

/**
 * Generar un intento de examen con resultados realistas
 * Las métricas se calcularán REALMENTE desde estos intentos
 */
function generateExamAttempt(uid, packId, attemptNumber, minCorrect, maxCorrect) {
  const totalQuestions = 10;
  
  // Generar número de aciertos aleatorio dentro del rango
  // Esto simula la variación real en el rendimiento del usuario
  const correctAnswers = Math.floor(Math.random() * (maxCorrect - minCorrect + 1)) + minCorrect;
  
  // Calcular métricas REALES desde los resultados
  const accuracy = (correctAnswers / totalQuestions) * 100;
  const xpEarned = correctAnswers * 10; // 10 XP por pregunta correcta
  
  const now = Date.now();
  const startedAt = now - (attemptNumber * 24 * 60 * 60 * 1000); // Diferentes días
  const durationMs = 15 * 60 * 1000 + Math.floor(Math.random() * 5 * 60 * 1000); // 15-20 min
  
  return {
    attemptId: `attempt_${uid}_${attemptNumber}`,
    uid: uid,
    packId: packId,
    startedAt: admin.firestore.Timestamp.fromMillis(startedAt),
    finishedAt: admin.firestore.Timestamp.fromMillis(startedAt + durationMs),
    durationMs: durationMs,
    status: 'COMPLETED',
    scoreRaw: correctAnswers,
    scoreValidated: correctAnswers,
    totalQuestions: totalQuestions,
    correctAnswers: correctAnswers,
    accuracy: Math.round(accuracy * 100) / 100, // Redondear a 2 decimales
    xpEarned: xpEarned,
    origin: 'OFFLINE',
    createdAt: admin.firestore.Timestamp.fromMillis(startedAt),
    updatedAt: admin.firestore.Timestamp.fromMillis(startedAt + durationMs)
  };
}

/**
 * Crear usuario con sus intentos de examen
 * Las métricas se calculan REALMENTE desde los intentos creados
 */
async function createUserWithAttempts(userData) {
  console.log(`\n👤 Creando usuario: ${userData.displayName} (${userData.uid})`);
  
  // Primero crear los intentos REALES
  const attempts = [];
  for (let i = 1; i <= userData.attempts; i++) {
    const attempt = generateExamAttempt(
      userData.uid,
      'pack_2025_w01',
      i,
      userData.minCorrect,
      userData.maxCorrect
    );
    attempts.push(attempt);
  }
  
  // Calcular métricas REALES desde los intentos creados
  let totalXp = 0;
  let totalCorrectAnswers = 0;
  let totalQuestions = 0;
  
  attempts.forEach(attempt => {
    totalXp += attempt.xpEarned;
    totalCorrectAnswers += attempt.correctAnswers;
    totalQuestions += attempt.totalQuestions;
  });
  
  // Calcular promedio REAL de aciertos
  const averageAccuracy = totalQuestions > 0
    ? Math.round((totalCorrectAnswers / totalQuestions) * 100 * 100) / 100
    : 0.0;
  
  // Crear documento del usuario
  const userRef = db.collection('users').doc(userData.uid);
  await userRef.set({
    uid: userData.uid,
    displayName: userData.displayName,
    email: userData.email,
    photoUrl: userData.photoUrl,
    schoolCode: userData.schoolCode,
    schoolId: `school_${userData.schoolCode}`,
    classroomId: `class_${userData.uid}`,
    coins: Math.floor(totalXp / 2), // Monedas basadas en XP
    selectedCosmeticId: null,
    
    // Métricas acumuladas
    totalXp: totalXp,
    averageAccuracy: averageAccuracy,
    totalAttempts: userData.attempts,
    totalCorrectAnswers: totalCorrectAnswers,
    totalQuestions: totalQuestions,
    
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  
  console.log(`   ✅ Usuario creado con métricas REALES calculadas desde ${userData.attempts} intentos:`);
  console.log(`      - Total XP: ${totalXp} (calculado desde intentos)`);
  console.log(`      - Promedio de Aciertos: ${averageAccuracy}% (calculado desde intentos)`);
  console.log(`      - Total Correctas: ${totalCorrectAnswers}/${totalQuestions}`);
  console.log(`      - Intentos completados: ${userData.attempts}`);
  
  // Crear intentos de examen
  console.log(`   📝 Creando ${attempts.length} intentos de examen...`);
  const batch = db.batch();
  
  attempts.forEach((attempt, index) => {
    const attemptRef = db.collection('exam_attempts').doc(attempt.attemptId);
    batch.set(attemptRef, attempt);
    
    if ((index + 1) % 10 === 0) {
      console.log(`      - ${index + 1}/${attempts.length} intentos preparados...`);
    }
  });
  
  await batch.commit();
  console.log(`   ✅ ${attempts.length} intentos creados`);
  
  return {
    uid: userData.uid,
    totalXp,
    averageAccuracy,
    attempts: userData.attempts
  };
}

/**
 * Función principal
 */
async function initUsersDataFirestore() {
  try {
    console.log('🚀 Iniciando creación de datos de ejemplo...');
    console.log(`📊 Proyecto: ${admin.app().options.projectId}`);
    console.log(`👥 Usuarios a crear: ${exampleUsers.length}`);
    
    const results = [];
    
    // Crear usuarios y sus intentos
    for (const userData of exampleUsers) {
      const result = await createUserWithAttempts(userData);
      results.push(result);
    }
    
    // Resumen
    console.log('\n📊 Resumen de datos creados:');
    console.log(`   👤 Usuarios: ${results.length}`);
    
    const totalAttempts = results.reduce((sum, r) => sum + r.attempts, 0);
    console.log(`   📝 Intentos de examen: ${totalAttempts}`);
    
    const totalXp = results.reduce((sum, r) => sum + r.totalXp, 0);
    console.log(`   ⭐ Total XP acumulado: ${totalXp}`);
    
    // Top 5 por XP
    console.log('\n🏆 Top 5 por XP:');
    results
      .sort((a, b) => b.totalXp - a.totalXp)
      .slice(0, 5)
      .forEach((user, index) => {
        const userData = exampleUsers.find(u => u.uid === user.uid);
        console.log(`   ${index + 1}. ${userData.displayName}: ${user.totalXp} XP (${user.averageAccuracy}%)`);
      });
    
    // Top 5 por Promedio de Aciertos
    console.log('\n🎯 Top 5 por Promedio de Aciertos:');
    results
      .sort((a, b) => b.averageAccuracy - a.averageAccuracy)
      .slice(0, 5)
      .forEach((user, index) => {
        const userData = exampleUsers.find(u => u.uid === user.uid);
        console.log(`   ${index + 1}. ${userData.displayName}: ${user.averageAccuracy}% (${user.totalXp} XP)`);
      });
    
    // Por Colegio
    console.log('\n🏫 Usuarios por Colegio:');
    const bySchool = {};
    results.forEach(user => {
      const userData = exampleUsers.find(u => u.uid === user.uid);
      const school = userData.schoolCode;
      if (!bySchool[school]) {
        bySchool[school] = [];
      }
      bySchool[school].push({ ...user, name: userData.displayName });
    });
    
    Object.keys(bySchool).sort().forEach(school => {
      console.log(`   ${school}: ${bySchool[school].length} usuarios`);
      bySchool[school]
        .sort((a, b) => b.totalXp - a.totalXp)
        .slice(0, 3)
        .forEach((user, index) => {
          console.log(`      ${index + 1}. ${user.name}: ${user.totalXp} XP`);
        });
    });
    
    console.log('\n✅ Datos de ejemplo creados exitosamente');
    console.log('\n💡 Puedes usar estos datos para probar el leaderboard en la app.');
    console.log('   Los usuarios tienen diferentes niveles de rendimiento y códigos de colegio.');
    
  } catch (error) {
    console.error('\n❌ Error al crear datos:', error);
    console.error('   Detalles:', error.message);
    process.exit(1);
  }
}

// Ejecutar
initUsersDataFirestore()
  .then(() => {
    console.log('\n✨ Proceso completado');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Error fatal:', error);
    process.exit(1);
  });

