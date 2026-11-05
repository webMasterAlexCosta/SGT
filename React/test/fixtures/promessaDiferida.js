export const diferida = () => {
  let resolve;
  let reject;
  const promise = new Promise((sucesso, falha) => { resolve = sucesso; reject = falha; });
  return { promise, resolve, reject };
};
