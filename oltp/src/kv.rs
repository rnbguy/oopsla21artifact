use serde::de::DeserializeOwned;
use serde::Serialize;
use std::collections::hash_map::DefaultHasher;
use std::fmt::Debug;
use std::hash::{Hash, Hasher};

pub type VarId = u64;
pub type Value = Vec<u8>;
pub type SetId = u64;
pub type SessionId = u64;
pub type TxnId = (SessionId, u64);
pub type OpId = (TxnId, u64);

fn compute_hash<K: Hash>(k: &K) -> u64 {
    let mut hasher = DefaultHasher::new();
    k.hash(&mut hasher);
    hasher.finish()
}

pub trait KVStoreT {
    fn commit(&mut self, id: &SessionId) -> bool;
    fn rollback(&mut self, id: &SessionId) -> bool;

    fn insert(&mut self, s: &SetId, x: &VarId, id: &SessionId) -> bool;
    fn delete(&mut self, s: &SetId, x: &VarId, id: &SessionId) -> bool;
    fn contains(&mut self, s: &SetId, x: &VarId, id: &SessionId) -> bool;
    fn scan(&mut self, s: SetId, id: &SessionId) -> Vec<VarId>;

    fn insert_with_name(&mut self, name: &str, x: &VarId, s_id: &SessionId) -> bool {
        // println!(
        //     "inserting {:?} with name {:?} with id {}",
        //     x,
        //     name,
        //     s_id
        // );
        self.insert(&compute_hash(&name.to_string()), x, s_id)
    }

    fn delete_with_name(&mut self, name: &str, x: &VarId, s_id: &SessionId) -> bool {
        // println!(
        //     "deleting {:?} with name {:?} with s_id {}",
        //     x,
        //     name,
        //     s_id
        // );
        self.delete(&compute_hash(&name.to_string()), x, s_id)
    }

    fn contains_with_name(&mut self, name: &str, x: &VarId, s_id: &SessionId) -> bool {
        // println!(
        //     "check for contain {:?} with name {:?} with s_id {}",
        //     x,
        //     name,
        //     s_id
        // );
        self.contains(&compute_hash(&name.to_string()), x, s_id)
    }

    fn scan_with_name(&mut self, name: &str, s_id: &SessionId) -> Vec<VarId> {
        // println!(
        //     "scan with name {:?} with s_id {}",
        //     name,
        //     s_id
        // );
        self.scan(compute_hash(&name.to_string()), s_id)
    }

    fn read_ser(&mut self, x: &VarId, s_id: &SessionId) -> Option<Value>;
    #[allow(clippy::ptr_arg)]
    fn write_ser(&mut self, x: &VarId, v: &Value, s_id: &SessionId) -> bool;

    fn read<K: Debug + Hash, V: DeserializeOwned>(&mut self, k: &K, s_id: &SessionId) -> Option<V> {
        // println!("reading key {:?} with s_id {}", k, s_id);
        let hash = compute_hash(k);
        // println!("reading key hash {:?}", hash);
        let v_raw = self.read_ser(&hash, s_id);
        v_raw.and_then(|v_raw| bincode::deserialize(&v_raw).ok())
    }

    fn write<K: Debug + Hash, V: Debug + Serialize>(
        &mut self,
        k: &K,
        v: &V,
        s_id: &SessionId,
    ) -> bool {
        // println!(
        //     "updating key {:?} (val {:?}) with id {}",
        //     k,
        //     v,
        //     s_id,
        // );
        let hash = compute_hash(&k);
        // println!("writing key hash {:?}", hash);
        self.write_ser(&hash, &bincode::serialize(v).unwrap(), s_id)
    }
}
