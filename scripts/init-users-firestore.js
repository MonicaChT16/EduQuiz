/**
 * Script para inicializar colecciones de usuarios y exam_attempts en Firestore
 * 
 * Uso:
 *   node scripts/init-users-firestore.js
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

/**
 * Crear usuario de ejemplo con métricas
 */
async function createExampleUser() {
  console.log('\n👤 Creando usuario de ejemplo...');
  
  const uid = 'user_example_001';
  const userRef = db.collection('users').doc(uid);
  
  await userRef.set({
    uid: uid,
    displayName: 'Juan Pérez',
    photoUrl: null,
    schoolCode: 'UGEL-001',  // Código ingresado manualmente
    schoolId: 'school1',
    classroomId: 'class1',
    coins: 500,
    selectedCosmeticId: null,
    
    // Métricas iniciales (se actualizarán automáticamente cuando complete exámenes)
    totalXp: 0,
    averageAccuracy: 0.0,
    totalAttempts: 0,
    totalCorrectAnswers: 0,
    totalQuestions: 0,
    
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  
  console.log(`   ✅ Usuario creado: ${uid}`);
  return uid;
}

/**
 * Crear intento de examen de ejemplo
 */
async function createExampleExamAttempt(uid) {
  console.log('\n📝 Creando intento de examen de ejemplo...');
  
  const attemptId = `attempt_${Date.now()}`;
  const attemptRef = db.collection('exam_attempts').doc(attemptId);
  
  await attemptRef.set({
    attemptId: attemptId,
    uid: uid,
    packId: 'pack_2025_w01',
    
    startedAt: admin.firestore.FieldValue.serverTimestamp(),
    finishedAt: admin.firestore.FieldValue.serverTimestamp(),
    durationMs: 1200000, // 20 minutos
    
    status: 'COMPLETED',
    scoreRaw: 8,
    scoreValidated: 8,
    totalQuestions: 10,
    correctAnswers: 8,
    accuracy: 80.0, // (8/10) * 100
    xpEarned: 80,
    
    origin: 'OFFLINE',
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  
  console.log(`   ✅ Intento creado: ${attemptId}`);
  console.log(`   📊 Resultado: 8/10 (80%) - XP ganado: 80`);
  
  return attemptId;
}

/**
 * Función principal
 */
async function initUsersFirestore() {
  try {
    console.log('🚀 Iniciando inicialización de usuarios y resultados en Firestore...');
    console.log(`📊 Proyecto: ${admin.app().options.projectId}`);
    
    // 1. Crear usuario de ejemplo
    const uid = await createExampleUser();
    
    // 2. Crear intento de examen de ejemplo
    const attemptId = await createExampleExamAttempt(uid);
    
    console.log('\n✅ Inicialización completada exitosamente');
    console.log('\n📋 Resumen:');
    console.log(`   👤 Usuario creado: ${uid}`);
    console.log(`   📝 Intento creado: ${attemptId}`);
    console.log('\n💡 Nota: Las métricas del usuario se actualizarán automáticamente');
    console.log('   cuando la Cloud Function se ejecute (si está desplegada).');
    console.log('   O puedes actualizarlas manualmente usando la función updateUserMetrics.');
    
  } catch (error) {
    console.error('\n❌ Error al inicializar:', error);
    console.error('   Detalles:', error.message);
    process.exit(1);
  }
}

// Ejecutar
initUsersFirestore()
  .then(() => {
    console.log('\n✨ Proceso completado');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Error fatal:', error);
    process.exit(1);
  });










