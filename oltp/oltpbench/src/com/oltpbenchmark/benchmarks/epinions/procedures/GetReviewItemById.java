/******************************************************************************
 *  Copyright 2015 by OLTPBenchmark Project                                   *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *    http://www.apache.org/licenses/LICENSE-2.0                              *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 ******************************************************************************/

package com.oltpbenchmark.benchmarks.epinions.procedures;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;

public class GetReviewItemById extends Procedure {

    // public final SQLStmt getReviewItem = new SQLStmt(
    //     "SELECT * FROM review r, item i WHERE i.i_id = r.i_id and r.i_id=?;"
    // );

    public final SQLStmt getReviewItem1 = new SQLStmt(
        "SELECT * FROM review WHERE i_id=?;"
    );

    public final SQLStmt getReviewItem2 = new SQLStmt(
        "SELECT * FROM item WHERE i_id=?;"
    );
    
    public void run(Connection conn, long iid) throws SQLException {
        PreparedStatement stmt1 = this.getPreparedStatement(conn, getReviewItem1);
        PreparedStatement stmt2 = this.getPreparedStatement(conn, getReviewItem2);
        stmt2.setLong(1, iid);
        ResultSet r= stmt2.executeQuery();
        while (r.next()) {
            continue;
        }
        r.close();
    }
    
}
