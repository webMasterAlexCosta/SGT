// Gerador local para conta sintética; nunca registrar argumento/código/segredo.
const crypto=require('node:crypto');
const totp=(secret,time=Date.now())=>{
  let accumulator=0,bits=0;const bytes=[];const alphabet='ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
  for(const char of secret.toUpperCase().replace(/=+$/,'')){
    const value=alphabet.indexOf(char);if(value<0)throw new Error('Formato Base32 inválido; valor omitido.');
    accumulator=(accumulator<<5)|value;bits+=5;if(bits>=8){bits-=8;bytes.push((accumulator>>>bits)&255);}
  }
  const counter=Buffer.alloc(8);counter.writeBigUInt64BE(BigInt(Math.floor(time/30000)));
  const mac=crypto.createHmac('sha1',Buffer.from(bytes)).update(counter).digest();const offset=mac[19]&15;
  return String((mac.readUInt32BE(offset)&0x7fffffff)%1000000).padStart(6,'0');
};
module.exports={totp};
